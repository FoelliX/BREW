package de.foellix.aql.brew;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.foellix.aql.Log;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.tpfpselector.Runner;
import de.foellix.aql.brew.tpfpselector.TPFP;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Attributes;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.JawaHelper;

public class Exporter {
	public static final int EXPORT_FORMAT_FLOWDROID_JIMPLE = 0;
	public static final int EXPORT_FORMAT_AMANDROID_JAWA = 1;

	private final File directory;
	private final File[] sourceAndSinkFiles;
	private final boolean[] emptyLinesAllowed;

	public Exporter(File outputDirectory) {
		this.directory = outputDirectory;
		this.sourceAndSinkFiles = new File[2];
		this.sourceAndSinkFiles[EXPORT_FORMAT_FLOWDROID_JIMPLE] = new File(this.directory, "SourcesAndSinks.txt");
		this.sourceAndSinkFiles[EXPORT_FORMAT_AMANDROID_JAWA] = new File(this.directory, "TaintSourcesAndSinks.txt");
		this.emptyLinesAllowed = new boolean[2];
		this.emptyLinesAllowed[EXPORT_FORMAT_FLOWDROID_JIMPLE] = true;
		this.emptyLinesAllowed[EXPORT_FORMAT_AMANDROID_JAWA] = false;
	}

	public void exportAnswers() {
		// Export expected AQL-Answers
		final File expected = new File(this.directory, "expected");
		if (!expected.exists()) {
			expected.mkdir();
		}
		int counter = 0;
		Stack<TPFP> currentList = new Stack<>();
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			counter++;
			currentList.push(tpfp);
			final Answer answer = assignAttributes(currentList, tpfp.toAnswer(), String.valueOf(tpfp.getId()));
			currentList.clear();
			AnswerHandler.createXML(answer, new File(expected, tpfp.getId() + "_"
					+ (tpfp.isTruepositive() ? "positive" : "negative") + "_case_" + getName(tpfp) + ".xml"));
		}
		Log.msg("All " + counter + " expected answers were exported to: " + expected.getAbsolutePath(), Log.NORMAL);

		// Export actual AQL-Answers
		final File actualDirectory = new File(this.directory, "actual");
		if (!actualDirectory.exists()) {
			actualDirectory.mkdir();
		}
		counter = 0;
		int counterCategorized = 0;
		currentList = new Stack<>();
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			if (!currentList.isEmpty() && (!getName(currentList.peek()).equals(getName(tpfp))
					&& currentList.peek().getActualAnswer() != tpfp.getActualAnswer())) {
				counter++;
				exportGeneral(actualDirectory, currentList);
				currentList = new Stack<>();
			}
			currentList.push(tpfp);
			counterCategorized++;
			exportCategorized(actualDirectory, currentList);
		}
		counter++;
		exportGeneral(actualDirectory, currentList);
		Log.msg("All " + counter + " summarized "
				+ (counterCategorized > 0 ? "and " + counterCategorized + " categorized " : "")
				+ "actual answers were exported to: " + actualDirectory.getAbsolutePath(), Log.NORMAL);
	}

	private void exportGeneral(File actualDirectory, Stack<TPFP> tpfps) {
		final int from = tpfps.firstElement().getId();
		final int to;
		if (tpfps.size() > 1) {
			to = tpfps.peek().getId();
		} else {
			to = -1;
		}

		if (tpfps.peek().getActualAnswer() != null) {
			final String caseID = from + (to == -1 ? "" : "-" + to);
			final Answer answer = assignAttributes(tpfps, tpfps.peek().getActualAnswer(), caseID);
			AnswerHandler.createXML(answer,
					new File(actualDirectory, caseID + "_case_" + getName(tpfps.peek()) + ".xml"));
		}
	}

	private void exportCategorized(File actualDirectory, Stack<TPFP> tpfps) {
		if (tpfps.peek().getStarted() != 0) {
			final String caseID = String.valueOf(tpfps.peek().getId());
			Answer answer;
			if (tpfps.peek().getActualAnswer() == null) {
				answer = new Answer();
			}

			File finalDirectory = null;
			File finalDirectoryFiltered = null;
			if (tpfps.peek().isTruepositive()) {
				if (tpfps.peek().getStatus() == TPFP.SUCCESSFUL) {
					// True Positive
					finalDirectory = new File(actualDirectory, "truePositive");
				} else {
					// False Negative
					finalDirectory = new File(actualDirectory, "falseNegative");
				}
				finalDirectoryFiltered = new File(finalDirectory, "filtered");
			}
			if (finalDirectory != null) {
				if (!finalDirectory.exists()) {
					finalDirectory.mkdir();
				}
				if (tpfps.peek().getStatus() == TPFP.SUCCESSFUL) {
					if (!finalDirectoryFiltered.exists()) {
						finalDirectoryFiltered.mkdir();
					}
					answer = assignAttributes(tpfps.peek(), tpfps.peek().getActualAnswer(), caseID);

					// Output filtered
					final Answer filtered = new Answer();
					final Flows flows = new Flows();
					flows.getFlow().addAll(Runner.getContained(answer, tpfps.peek().toAnswer()));
					filtered.setFlows(flows);
					AnswerHandler.createXML(filtered,
							new File(finalDirectoryFiltered, caseID + "_case_" + getName(tpfps.peek()) + ".xml"));
				}
				answer = assignAttributes(tpfps, tpfps.peek().getActualAnswer(), caseID);
				AnswerHandler.createXML(answer,
						new File(finalDirectory, caseID + "_case_" + getName(tpfps.peek()) + ".xml"));
			}

			finalDirectory = null;
			if (tpfps.peek().isFalsepositive()) {
				if (tpfps.peek().getStatus() == TPFP.SUCCESSFUL) {
					// True Negative
					finalDirectory = new File(actualDirectory, "trueNegative");
				} else {
					// False Positive
					finalDirectory = new File(actualDirectory, "falsePositive");
				}
				finalDirectoryFiltered = new File(finalDirectory, "filtered");
			}
			if (finalDirectory != null) {
				if (!finalDirectory.exists()) {
					finalDirectory.mkdir();
				}
				if (tpfps.peek().getStatus() != TPFP.SUCCESSFUL) {
					if (!finalDirectoryFiltered.exists()) {
						finalDirectoryFiltered.mkdir();
					}
					answer = assignAttributes(tpfps.peek(), tpfps.peek().getActualAnswer(), caseID);

					// Output filtered
					final Answer filtered = new Answer();
					final Flows flows = new Flows();
					flows.getFlow().addAll(Runner.getContained(answer, tpfps.peek().toAnswer()));
					filtered.setFlows(flows);
					AnswerHandler.createXML(filtered,
							new File(finalDirectoryFiltered, caseID + "_case_" + getName(tpfps.peek()) + ".xml"));
				}
				answer = assignAttributes(tpfps, tpfps.peek().getActualAnswer(), caseID);
				AnswerHandler.createXML(answer,
						new File(finalDirectory, caseID + "_case_" + getName(tpfps.peek()) + ".xml"));
			}
		}
	}

	public static Answer assignAttributes(TPFP tpfp, Answer answer, String caseID) {
		final Stack<TPFP> temp = new Stack<>();
		temp.push(tpfp);
		return assignAttributes(temp, answer, caseID);
	}

	public static Answer assignAttributes(Stack<TPFP> tpfps, Answer answer, String caseID) {
		if (answer != null && answer.getFlows() != null && !answer.getFlows().getFlow().isEmpty()) {
			for (int flowID = 0; flowID < answer.getFlows().getFlow().size(); flowID++) {
				final Flow flow = answer.getFlows().getFlow().get(flowID);
				flow.setAttributes(new Attributes());

				final Attribute caseIDAttr = new Attribute();
				caseIDAttr.setName("CaseID");
				caseIDAttr.setValue(String.valueOf(caseID));
				flow.getAttributes().getAttribute().add(caseIDAttr);

				final Attribute flowIDAttr = new Attribute();
				flowIDAttr.setName("FlowID");
				flowIDAttr.setValue(String.valueOf(flowID + 1));
				flow.getAttributes().getAttribute().add(flowIDAttr);

				for (final TPFP tpfp : tpfps) {
					for (final String key : tpfp.getAttributes().keySet()) {
						final String value = tpfp.getAttributes().get(key);
						boolean found = false;

						// Check if already added
						for (final Attribute attr : flow.getAttributes().getAttribute()) {
							if (attr.getName().equals(key) && attr.getValue().equals(value)) {
								found = true;
								break;
							}
						}

						// Add if not there yet
						if (!found) {
							final Attribute tpfpAttr = new Attribute();
							tpfpAttr.setName(key);
							tpfpAttr.setValue(value);
							flow.getAttributes().getAttribute().add(tpfpAttr);
						}
					}
				}
			}
		}
		return answer;
	}

	private String getName(TPFP tpfp) {
		String name = tpfp.getBenchmarkcase();
		int from = name.lastIndexOf("/");
		if (from < 0) {
			from = name.lastIndexOf("\\\\");
		}
		final int to = name.lastIndexOf(".apk");
		if (from > 0 && to > 0) {
			name = name.substring(from + 1, to);
		} else {
			name = name.replaceAll("[^A-Za-z0-9]", "");
		}
		return name;
	}

	public void exportSourcesAndSinksAllFormats(List<SourceOrSink> sourcesAndSinks) {
		exportSourcesAndSinks(sourcesAndSinks, EXPORT_FORMAT_FLOWDROID_JIMPLE);
		exportSourcesAndSinks(sourcesAndSinks, EXPORT_FORMAT_AMANDROID_JAWA);
	}

	public void exportSourcesAndSinks(List<SourceOrSink> sourcesAndSinks, int exportFormat) {
		switch (exportFormat) {
		case EXPORT_FORMAT_AMANDROID_JAWA:
			exportSourcesAndSinksJawa(sourcesAndSinks);
			break;
		case EXPORT_FORMAT_FLOWDROID_JIMPLE:
			exportSourcesAndSinksJimple(sourcesAndSinks);
			break;
		default:
			exportSourcesAndSinksJimple(sourcesAndSinks);
			break;
		}
	}

	private void exportSourcesAndSinksJimple(List<SourceOrSink> sourcesAndSinks) {
		// Collect
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final SourceOrSink ss : sourcesAndSinks) {
			if (ss.isSource()) {
				exportSourceOrSinkJimple(sourcesAndSinksToExport, ss, true);
			}
			if (ss.isSink()) {
				exportSourceOrSinkJimple(sourcesAndSinksToExport, ss, false);
			}
		}

		// Output
		exportToFile(sourcesAndSinksToExport, EXPORT_FORMAT_FLOWDROID_JIMPLE);
	}

	private void exportSourceOrSinkJimple(Set<String> sourcesAndSinksToExport, SourceOrSink ss, boolean isSource) {
		sourcesAndSinksToExport.add("<" + ss.getStatement() + "> -> "
				+ (isSource ? StringConstants.SOURCE_IDENTIFIER : StringConstants.SINK_IDENTIFIER));
		if (ss.getCombine() != null && !ss.getCombine().isEmpty()) {
			for (final String idStr : ss.getCombine().replaceAll(" ", "").split(",")) {
				if (!idStr.isEmpty()) {
					final int id = Integer.valueOf(idStr).intValue();
					for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
						if (id == item.getId()) {
							sourcesAndSinksToExport.add("<" + item.getStatement() + "> -> "
									+ (isSource ? StringConstants.SOURCE_IDENTIFIER : StringConstants.SINK_IDENTIFIER));
							break;
						}
					}
				}
			}
		}
	}

	private void exportSourcesAndSinksJawa(List<SourceOrSink> sourcesAndSinks) {
		// Collect
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final SourceOrSink ss : sourcesAndSinks) {
			if (ss.isSource()) {
				exportSourceOrSinkJawa(sourcesAndSinksToExport, ss, true);
			}
			if (ss.isSink()) {
				exportSourceOrSinkJawa(sourcesAndSinksToExport, ss, false);
			}
		}

		// Output
		exportToFile(sourcesAndSinksToExport, EXPORT_FORMAT_AMANDROID_JAWA);
	}

	private void exportSourceOrSinkJawa(Set<String> sourcesAndSinksToExport, SourceOrSink ss, boolean isSource) {
		if (isSource) {
			sourcesAndSinksToExport.add(JawaHelper.toJawa(ss.getReference().getStatement()) + " SENSITIVE_INFO -> "
					+ StringConstants.SOURCE_IDENTIFIER);
		} else {
			sourcesAndSinksToExport.add(buildSinkJawa(ss));
		}
		if (ss.getCombine() != null && !ss.getCombine().isEmpty()) {
			for (final String idStr : ss.getCombine().replaceAll(" ", "").split(",")) {
				if (!idStr.isEmpty()) {
					final int id = Integer.valueOf(idStr).intValue();
					for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
						if (id == item.getId()) {
							if (isSource) {
								sourcesAndSinksToExport.add(JawaHelper.toJawa(item.getReference().getStatement())
										+ " SENSITIVE_INFO -> " + StringConstants.SOURCE_IDENTIFIER);
							} else {
								sourcesAndSinksToExport.add(buildSinkJawa(item));
							}
							break;
						}
					}
				}
			}
		}
	}

	private String buildSinkJawa(SourceOrSink ss) {
		final StringBuilder attachment = new StringBuilder();
		if (ss.getReference().getStatement().getParameters() != null
				&& !ss.getReference().getStatement().getParameters().getParameter().isEmpty()) {
			attachment.append(" ");
			for (int i = 1; i <= ss.getReference().getStatement().getParameters().getParameter().size(); i++) {
				attachment.append((attachment.length() == 1 ? "" : "|") + i);
			}
		}
		return JawaHelper.toJawa(ss.getReference().getStatement()) + " -> " + StringConstants.SINK_IDENTIFIER
				+ attachment.toString();
	}

	private void exportToFile(Set<String> sourcesAndSinksToExport, int exportFormat) {
		Log.msg("Exporting sources and sinks:", Log.NORMAL);
		final List<String> sourcesAndSinksToExportSorted = new ArrayList<String>(sourcesAndSinksToExport);
		Collections.sort(sourcesAndSinksToExportSorted);
		try (FileWriter fw = new FileWriter(this.sourceAndSinkFiles[exportFormat])) {
			// Sources
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(StringConstants.SOURCE_IDENTIFIER)
						|| s.substring(0, s.lastIndexOf(' ')).endsWith(StringConstants.SOURCE_IDENTIFIER)) {
					Log.msg("\t- " + s, Log.DEBUG);
					fw.write(s + "\n");
				}
			}

			if (this.emptyLinesAllowed[exportFormat]) {
				// Add empty line between sources and sinks
				fw.write("\n");
			}

			// Sinks
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(StringConstants.SINK_IDENTIFIER)
						|| s.substring(0, s.lastIndexOf(' ')).endsWith(StringConstants.SINK_IDENTIFIER)) {
					Log.msg("\t- " + s, Log.DEBUG);
					fw.write(s + "\n");
				}
			}

			Log.msg("Sources and sinks successfully exported to: "
					+ this.sourceAndSinkFiles[exportFormat].getAbsolutePath(), Log.NORMAL);
		} catch (final IOException e) {
			Log.error("Could not write sources and sinks file: "
					+ this.sourceAndSinkFiles[exportFormat].getAbsolutePath() + Log.getExceptionAppendix(e));
		}
	}

	public File getOutputFile(int exportFormat) {
		return this.sourceAndSinkFiles[exportFormat];
	}
}