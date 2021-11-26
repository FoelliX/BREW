package de.foellix.aql.brew.taintbench;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.foellix.aql.Log;
import de.foellix.aql.brew.BREW;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.config.Config;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.taintbench.datastructure.Finding;
import de.foellix.aql.brew.taintbench.datastructure.TaintBenchCase;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.brew.tpfpselector.TPFP;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.helper.Helper;

public class TaintBenchLoader {
	public static final String TAINTBENCH_ID = "TaintBenchID";
	public static final String TAINTBENCH_DIR = "TAINTBENCH_DIR";

	protected static final String TARGET_NOT_SPECIFIED = "target not specified";

	private SourceOrSinkMapper sasm;

	private File taintBenchFindingsDirectory;
	private File taintBenchAppsDirectory;

	private Map<TPFP, Finding> writeBackMap;
	private Map<TaintBenchCase, File> writeBackFiles;
	private List<TPFP> tpfps = null;

	public TaintBenchLoader(File taintBenchFindingsDirectory, File taintBenchAppsDirectory) {
		// Set inputs
		this.taintBenchFindingsDirectory = taintBenchFindingsDirectory;
		this.taintBenchAppsDirectory = taintBenchAppsDirectory;
		this.writeBackMap = new HashMap<>();
		this.writeBackFiles = new HashMap<>();
		this.sasm = new SourceOrSinkMapper();

		// Load inputs
		loadTaintBenchFindings();
	}

	/*
	 * Findings
	 */
	public void loadTaintBenchFindings() {
		Log.msg("TaintBench (Step 1 of 3): Loading", Log.NORMAL);
		this.tpfps = new ArrayList<>();
		final List<TaintBenchCase> cases = readDirectory(this.taintBenchFindingsDirectory);
		int counter = 0;
		int findingsCounterPos = 0;
		int findingsCounterNeg = 0;
		for (final TaintBenchCase taintBenchCase : cases) {
			Log.msg("Loading (" + ++counter + " of " + cases.size() + ")", Log.NORMAL);
			final File apk = FileRelocator.recursivelySearchFile(new File(taintBenchCase.getFileName()),
					this.taintBenchAppsDirectory, true);
			if (apk != null) {
				final App app = Helper.createApp(apk);

				for (final Finding finding : taintBenchCase.getFindings()) {
					if (finding.getSource() == null || finding.getSink() == null) {
						Log.warning("Could not read finding (ID: " + finding.getID() + ") for "
								+ taintBenchCase.getFileName() + ": "
								+ (finding.getSource() == null
										? (finding.getSink() == null ? "Source and sink missing" : "Source missing")
										: "Sink missing"));
						continue;
					}

					// Source
					final Reference refSource = new Reference();
					refSource.setApp(app);
					refSource.setClassname(finding.getSource().getClassName());
					refSource.setMethod(finding.getSource().getMethodName());
					refSource.setStatement(TaintBenchHelper.createStatement(finding.getSource().getStatement(),
							finding.getSource().getLineNo()));
					if (finding.getSource().getTargetName() != null && !finding.getSource().getTargetName().isEmpty()) {
						refSource.getStatement().setStatementfull(refSource.getStatement().getStatementfull() + " -> "
								+ finding.getSource().getTargetName() + " [" + finding.getSource().getTargetNo() + "]");
					} else {
						refSource.getStatement().setStatementfull(
								refSource.getStatement().getStatementfull() + " -> " + TARGET_NOT_SPECIFIED);
					}
					final SourceOrSink source = new SourceOrSink(true, false, refSource);

					// Sink
					final Reference refSink = new Reference();
					refSink.setApp(app);
					refSink.setClassname(finding.getSink().getClassName());
					refSink.setMethod(finding.getSink().getMethodName());
					refSink.setStatement(TaintBenchHelper.createStatement(finding.getSink().getStatement(),
							finding.getSink().getLineNo()));
					if (finding.getSink().getTargetName() != null && !finding.getSink().getTargetName().isEmpty()) {
						refSink.getStatement().setStatementfull(refSink.getStatement().getStatementfull() + " -> "
								+ finding.getSink().getTargetName() + " [" + finding.getSink().getTargetNo() + "]");
					} else {
						refSink.getStatement().setStatementfull(
								refSink.getStatement().getStatementfull() + " -> target not specified");
					}
					final SourceOrSink sink = new SourceOrSink(true, false, refSink);

					// TPFP
					final TPFP tpfp = new TPFP(source, sink);
					if (!finding.getIsNegative()) {
						findingsCounterPos++;
						tpfp.setTruepositive(true);
						tpfp.setFalsepositive(false);
					} else {
						findingsCounterNeg++;
						tpfp.setTruepositive(false);
						tpfp.setFalsepositive(true);
					}
					tpfp.getAttributes().put(TAINTBENCH_ID, String.valueOf(finding.getID()));
					this.tpfps.add(tpfp);

					// Write-back
					if (BREW.getTaintBenchWriteBack()) {
						this.writeBackMap.put(tpfp, finding);
					}
				}
			}
		}
		Log.msg("Parsed " + (findingsCounterPos + findingsCounterNeg) + " (positive: " + findingsCounterPos
				+ ", negative: " + findingsCounterNeg + ") TaintBench findings.", Log.NORMAL);
	}

	private List<TaintBenchCase> readDirectory(File taintBenchDirectory) {
		final List<TaintBenchCase> cases = new ArrayList<>();
		for (final File taintBenchCase : taintBenchDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".json");
			}
		})) {
			if (taintBenchCase.isDirectory()) {
				cases.addAll(readDirectory(taintBenchCase));
			} else {
				cases.add(readFile(taintBenchCase));
			}
		}
		return cases;
	}

	private TaintBenchCase readFile(File jsonFile) {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			final TaintBenchCase tbCase = mapper.readValue(jsonFile, TaintBenchCase.class);
			this.writeBackFiles.put(tbCase, jsonFile);
			return tbCase;
		} catch (final IOException e) {
			Log.error("Problem occured while reading TaintBench file: " + jsonFile + Log.getExceptionAppendix(e));
			return null;
		}
	}

	/*
	 * Sources and Sinks
	 */
	public void applyToSourcesAndSinks() {
		Log.msg("TaintBench (Step 2 of 3): Applying - Sources & Sinks", Log.NORMAL);
		Data.getInstance().deselectAllSourcesAndSinks();
		final Map<String, List<String>> duplicates = new HashMap<>();
		final List<TPFP> correctedTPFPs = new ArrayList<>();
		int counter = 0;
		final Set<SourceOrSink> findingsSources = new HashSet<>();
		final Set<SourceOrSink> findingsSinks = new HashSet<>();
		boolean skipped = false;
		for (final TPFP tpfp : this.tpfps) {
			if (testcaseAvailable(tpfp.getFrom().getReference().getApp())) {
				Log.msg("Applying - Sources & Sinks (" + ++counter + " of " + this.tpfps.size() + ")", Log.NORMAL);

				final SourceOrSink from = this.sasm.findSourceOrSink(tpfp.getFrom(), true);
				Log.msg("Source: " + from + " (" + tpfp.getFrom() + " - " + from.getReference().getApp().getFile()
						+ ")", Log.DEBUG_DETAILED);
				from.setSource(true);
				if (!findingsSources.contains(from)) {
					findingsSources.add(from);
				}
				final SourceOrSink to = this.sasm.findSourceOrSink(tpfp.getTo(), false);
				Log.msg("Sink:   " + to + " (" + tpfp.getTo() + " - " + to.getReference().getApp().getFile() + ")",
						Log.DEBUG_DETAILED);
				to.setSink(true);
				if (!findingsSinks.contains(to)) {
					findingsSinks.add(to);
				}

				// Identify duplicates
				final String identifier = from.getId() + "-" + to.getId() + "-"
						+ Helper.toRAW(from.getReference().getApp());
				final String value = tpfp.getAttributes().get("TaintBenchID");
				if (duplicates.containsKey(identifier)) {
					duplicates.get(identifier).add(value);
				} else {
					final List<String> temp = new ArrayList<>();
					temp.add(from.getReference().getApp().getFile());
					temp.add(value);
					duplicates.put(identifier, temp);
				}
				// ---

				final TPFP correctedTPFP = new TPFP(from, to);
				correctedTPFP.setTruepositive(tpfp.isTruepositive());
				correctedTPFP.setFalsepositive(tpfp.isFalsepositive());
				correctedTPFP.setAttributes(tpfp.getAttributes());
				correctedTPFPs.add(correctedTPFP);

				// Write-back
				if (BREW.getTaintBenchWriteBack()) {
					TaintBenchHelper.writeBack(this.writeBackMap.get(tpfp).getSource(),
							from.getReference().getStatement().getStatementfull());
					TaintBenchHelper.writeBack(this.writeBackMap.get(tpfp).getSink(),
							to.getReference().getStatement().getStatementfull());
				}
			} else {
				counter++;
				skipped = true;
			}
		}

		// Write-back to file
		if (BREW.getTaintBenchWriteBack()) {
			for (final TaintBenchCase tbCase : this.writeBackFiles.keySet()) {
				final ObjectMapper mapper = new ObjectMapper();
				try {
					final File newFile = new File(this.writeBackFiles.get(tbCase).getParentFile(),
							this.writeBackFiles.get(tbCase).getName());
					final String jsonContent = mapper.writeValueAsString(tbCase);

					// Pretty print by gson
					final FileWriter writer = new FileWriter(newFile);
					JsonObject gsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
					gsonObject = removeFalseAttributes(gsonObject);
					final Gson gsonPrettyPrinter = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
					writer.write(gsonPrettyPrinter.toJson(gsonObject));
					writer.close();

					Log.msg("Writing jimple statements to: " + newFile.getAbsolutePath(), Log.NORMAL);
				} catch (final IOException e) {
					Log.error("Could not write TaintBench findings file: "
							+ this.writeBackFiles.get(tbCase).getAbsolutePath() + Log.getExceptionAppendix(e));
				}
			}
		}

		// Output
		for (final String key : duplicates.keySet()) {
			if (duplicates.get(key).size() > 2) {
				String duplicateFound = "";
				for (final String taintBenchID : duplicates.get(key)) {
					duplicateFound += taintBenchID + (duplicateFound.length() == 0 ? " -> " : ", ");
				}
				duplicateFound = duplicateFound.substring(0, duplicateFound.length() - 2);
				Log.msg("Duplicate finding detected (may differ in intermediate flow): " + duplicateFound, Log.DEBUG);
			}
		}
		Log.msg("Found " + findingsSources.size() + " TaintBench sources and " + findingsSinks.size()
				+ " TaintBench sinks.", Log.NORMAL);
		if (skipped) {
			Log.msg("(All other Sources & Sinks were skipped since no associated testcase could be found.)",
					Log.NORMAL);
		}
		this.tpfps = correctedTPFPs;

		Data.getInstance().setSourcesAndSinksChangedFlag(true);
	}

	private JsonObject removeFalseAttributes(JsonObject obj) {
		final JsonArray findings = obj.getAsJsonArray("findings");
		for (final Iterator<JsonElement> findingsIterator = findings.iterator(); findingsIterator.hasNext();) {
			final JsonObject finding = (JsonObject) findingsIterator.next();
			if (finding.has("attributes")) {
				final JsonObject attrs = finding.getAsJsonObject("attributes");
				final List<String> toRemove = new ArrayList<>();
				for (final Entry<String, JsonElement> attr : attrs.entrySet()) {
					if (attr.getValue().toString().equals("false")) {
						toRemove.add(attr.getKey());
					}
				}
				for (final String remove : toRemove) {
					attrs.remove(remove);
				}
			}
		}
		return obj;
	}

	private Testcase getTestcase(App app) {
		for (final Testcase tc : Data.getInstance().getTestcases()) {
			if (tc.getApk().equals(new File(app.getFile()))) {
				return tc;
			}
		}
		return null;
	}

	private boolean testcaseAvailable(App app) {
		return getTestcase(app) != null;
	}

	/*
	 * TPFPs
	 */
	public void applyToTPFPs(boolean keepGenericNegativeCases) {
		Log.msg("TaintBench (Step 3 of 3): Applying - TPFPs", Log.NORMAL);

		// Initialize
		for (final TPFP output : Data.getInstance().getTPFPList()) {
			output.setTruepositive(false);
			output.setFalsepositive(true);
		}

		// Count duplicates
		final Set<Integer> duplicates = new HashSet<>();
		for (final TPFP test : this.tpfps) {
			for (final TPFP test2 : this.tpfps) {
				if (test.getId() < test2.getId() && test.equals(test2)) {
					duplicates.add(test.getId());
				}
			}
		}

		// Set according to TaintBench
		int counter = 0;
		int countPos = 0;
		int countNeg = 0;
		final Set<Integer> toSelfs = new HashSet<>();
		final List<TPFP> alreadyFound = new ArrayList<>();
		for (final TPFP input : this.tpfps) {
			Log.msg("Applying - TPFPs (" + ++counter + " of " + this.tpfps.size() + ")", Log.NORMAL);
			boolean found = false;
			for (final TPFP output : Data.getInstance().getTPFPList()) {
				if (EqualsHelper.equals(input.getFrom().getReference(), output.getFrom().getReference(),
						EqualsOptions.DEFAULT.setOption(EqualsOptions.PRECISELY_REFERENCE, true))
						&& EqualsHelper.equals(input.getTo().getReference(), output.getTo().getReference(),
								EqualsOptions.DEFAULT.setOption(EqualsOptions.PRECISELY_REFERENCE, true))) {
					if (!alreadyFound.contains(output)) {
						if (input.isTruepositive()) {
							output.setTruepositive(true);
							output.setFalsepositive(false);
							countPos++;
						} else {
							output.setTruepositive(false);
							output.setFalsepositive(true);
							countNeg++;
						}
						output.setAttributes(input.getAttributes());
						alreadyFound.add(output);
					}
					found = true;
					break;
				}
			}
			if (!found) {
				if (input.getFrom() == input.getTo()) {
					toSelfs.add(input.getId());
				} else {
					Log.msg("\"" + input.getCase() + "\":\n\t" + Helper.toString(input.getFrom().getReference())
							+ "\n->\n\t" + Helper.toString(input.getFrom().getReference()) + "\nnot found!",
							Log.NORMAL);
				}
			}
		}
		final List<TPFP> tpfpNonGenericNegativeCases = new ArrayList<>();
		for (final TPFP tpfp : Data.getInstance().getTPFPList()) {
			if (!tpfp.getAttributes().containsKey(TAINTBENCH_ID)) {
				if (keepGenericNegativeCases) {
					tpfp.getAttributes().put(TAINTBENCH_ID, String.valueOf(-1));
				}
			} else {
				tpfpNonGenericNegativeCases.add(tpfp);
			}
			tpfp.getAttributes().put(TAINTBENCH_DIR, this.taintBenchFindingsDirectory.getAbsolutePath());
		}
		if (!keepGenericNegativeCases && !tpfpNonGenericNegativeCases.isEmpty()) {
			Log.msg("Keeping " + tpfpNonGenericNegativeCases.size() + " non-generic testcases.", Log.NORMAL);
			Data.getInstance().getTPFPs().clear();
			Data.getInstance().getTPFPs().addAll(tpfpNonGenericNegativeCases);
		}

		// Log result
		if (Log.logIt(Log.DEBUG_DETAILED)) {
			if (!duplicates.isEmpty()) {
				Log.msg("List of duplicates (source or sink ambiguous because of Jimple representation - statement \"look\" same although found in different lines.):",
						Log.DEBUG_DETAILED);
				int i = 0;
				for (final TPFP duplicate : this.tpfps) {
					if (duplicates.contains(duplicate.getId())) {
						Log.msg(++i + " - " + duplicate.getId() + ")\nSource (" + duplicate.getFrom().getId() + "): "
								+ Helper.toString(duplicate.getFrom().getReference()) + "\nSink ("
								+ duplicate.getTo().getId() + "): " + Helper.toString(duplicate.getTo().getReference())
								+ "\n", Log.DEBUG_DETAILED);
					}
				}
			}

			if (!toSelfs.isEmpty()) {
				Log.msg("List of identity flows:", Log.DEBUG_DETAILED);
				int i = 0;
				for (final TPFP toSelf : this.tpfps) {
					if (toSelfs.contains(toSelf.getId())) {
						Log.msg(++i + " - " + toSelf.getId() + ")\nSource (" + toSelf.getFrom().getId() + "): "
								+ Helper.toString(toSelf.getFrom().getReference()) + "\nSink (" + toSelf.getTo().getId()
								+ "): " + Helper.toString(toSelf.getTo().getReference()) + "\n", Log.DEBUG_DETAILED);
					}
				}
			}
		}

		Log.msg("Found " + alreadyFound.size() + " of " + (this.tpfps.size() - duplicates.size() - toSelfs.size())
				+ " (" + this.tpfps.size() + " [overall] - " + duplicates.size() + " [duplicates] - " + toSelfs.size()
				+ " [flows from and to the same statement] = "
				+ (this.tpfps.size() - duplicates.size() - toSelfs.size()) + ") cases (positive: " + countPos
				+ ", negative: " + countNeg + ") defined in TaintBench.", Log.NORMAL);
	}

	public static void relocate() {
		final File relocationDir = new File(Config.getInstance().get(Config.AUTOMATIC_TAINTBENCH_RELOCATION_DIR));
		if (relocationDir.exists()) {
			for (final TPFP tpfp : Data.getInstance().getTPFPList()) {
				if (tpfp.getAttributes().containsKey(TAINTBENCH_DIR)) {
					tpfp.getAttributes().replace(TAINTBENCH_DIR, relocationDir.getAbsolutePath());
				} else {
					tpfp.getAttributes().put(TAINTBENCH_DIR, relocationDir.getAbsolutePath());
				}
			}
			Log.msg("Relocated TaintBench directory to: " + relocationDir.getAbsolutePath(), Log.NORMAL);
		}
	}
}