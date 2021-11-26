package de.foellix.aql.brew.sourceandsinkselector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.MenuBar;
import de.foellix.aql.brew.SootHelper;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Hash;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.ui.gui.ProgressDialog;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;

public class SourceAndSinkExtractor {
	private final FileRelocator relocator;

	private int totalMax;
	private int totalDone;
	private int currentMax;
	private int currentDone;

	SourceAndSinkExtractor(Stage stage) {
		this.relocator = new FileRelocator(stage);
	}

	public void extractAll(SourceAndSinkSelector sourceAndSinkSelector) {
		boolean skip = false;
		for (final Testcase testcase : Data.getInstance().getTestcases()) {
			if (!testcase.getApk().exists()) {
				final File relocatedFile = this.relocator.relocateFile(testcase.getApk());
				if (relocatedFile != null) {
					testcase.setApk(relocatedFile);
				} else {
					Log.warning(
							"Sources and Sinks cannot be refreshed since some apps cannot be found/relocated (e.g.: "
									+ testcase.getApk() + ").");
					skip = true;
					break;
				}
			}
		}

		if (!skip) {
			final List<SourceOrSink> oldList = new ArrayList<>();
			oldList.addAll(Data.getInstance().getSourcesAndSinks());

			Data.getInstance().getSourcesAndSinks().clear();
			Data.getInstance().getSourceAndSinkList().clear();
			final Map<Testcase, List<SourceOrSink>> newMap = new HashMap<>();
			final Map<SourceOrSink, Testcase> newMapR = new HashMap<>();

			final ProgressDialog progressDialog = new ProgressDialog();
			this.totalMax = Math.max(Data.getInstance().getTestcases().size(), 1);
			this.totalDone = 0;
			progressDialog.updateProgress(0, 100, this.totalDone, this.totalMax);

			new Thread(() -> {
				for (final Testcase testcase : Data.getInstance().getTestcases()) {
					final Set<SourceOrSink> currentSet = new HashSet<>();
					final App app = Helper.createApp(testcase.getApk());
					Reference reference = null;
					if (!contains(testcase)) {
						// Extract
						Log.msg("Determining sources & sinks for: " + testcase.getId() + ": " + testcase.getName(),
								Log.NORMAL);
						final Collection<SootClass> classes = SootHelper.getInstance().extract(testcase);
						int classCounter = 0;
						for (final SootClass c : classes) {
							progressDialog.updateProgress(classCounter++, classes.size(), this.totalDone,
									this.totalMax);
							for (final SootMethod m : c.getMethods()) {
								if (m.isConcrete()) {
									final String methodSignature = m.getSignature();
									final Body b = m.retrieveActiveBody();
									final Set<SourceOrSink> checkSet = new HashSet<>();
									for (final Unit u : b.getUnits()) {
										final String temp = u.toString();
										if (((u instanceof InvokeStmt) || ((u instanceof AssignStmt)
												&& (((AssignStmt) u).containsInvokeExpr())))
												&& ((!temp.contains(KeywordsAndConstantsHelper.CONSTRUCTOR_NAME))
														|| (!temp.contains("android.content.Intent: void <init>()")
																&& !temp.contains(
																		"android.content.IntentFilter: void <init>()")
																&& !temp.contains(
																		"android.content.BroadcastReceiver: void <init>()")
																&& !temp.contains(
																		"android.content.ContentProvider: void <init>()")
																&& !temp.contains("android.app.Activity: void <init>()")
																&& !temp.contains("android.app.Service: void <init>()")
																&& !temp.contains(
																		"java.lang.Object: void <init>()")))) {
											reference = new Reference();
											reference.setApp(app);
											reference.setClassname(c.getName());
											reference.setMethod(methodSignature);
											reference.setStatement(
													Helper.createStatement(temp, u.getJavaSourceStartLineNumber()));

											final SourceOrSink sourceOrSink = new SourceOrSink(false, false, reference);
											if (oldList != null && !oldList.isEmpty()) {
												restore(sourceOrSink, oldList);
											}
											if (!checkSet.contains(sourceOrSink)) {
												Data.getInstance().getSourceAndSinkList().add(sourceOrSink);
												checkSet.add(sourceOrSink);
												currentSet.add(sourceOrSink);

												if (newMap.get(testcase) != null) {
													newMap.get(testcase).add(sourceOrSink);
												} else {
													final List<SourceOrSink> initialList = new ArrayList<>();
													initialList.add(sourceOrSink);
													newMap.put(testcase, initialList);
												}
												newMapR.put(sourceOrSink, testcase);
											}
										}
									}
								}
							}
						}

						// Load given ground truth AQL-Answer
						{
							File givenGroundTruth;

							// Same name as APK
							if (testcase.getApk().getName().endsWith(".apk")) {
								givenGroundTruth = new File(testcase.getApk().getParentFile(),
										testcase.getApk().getName().replace(".apk", ".xml"));
							} else {
								givenGroundTruth = new File(testcase.getApk().getParentFile(),
										testcase.getApk().getName() + ".xml");
							}

							// Otherwise use ground-truth.xml
							if (!givenGroundTruth.exists()) {
								givenGroundTruth = new File(testcase.getApk().getParentFile(), "ground-truth.xml");
							}

							// If given ground truth available
							if (givenGroundTruth.exists()) {
								final Answer answer = AnswerHandler.parseXML(givenGroundTruth);
								if (answer.getFlows() != null && !answer.getFlows().getFlow().isEmpty()) {
									for (final Flow flow : answer.getFlows().getFlow()) {
										final Reference source = Helper.getFrom(flow);
										source.getStatement().setStatementgeneric(
												Helper.cleanupParameters(source.getStatement().getStatementgeneric()));
										final Reference sink = Helper.getTo(flow);
										sink.getStatement().setStatementgeneric(
												Helper.cleanupParameters(sink.getStatement().getStatementgeneric()));
										for (final SourceOrSink needle : currentSet) {
											if (EqualsHelper.equals(needle.getReference(), source)) {
												needle.setSource(true);
											}
											if (EqualsHelper.equals(needle.getReference(), sink)) {
												needle.setSink(true);
											}
											final StringBuilder sb = new StringBuilder(
													"Selecting \"" + Helper.toString(needle) + "\" as ");
											if (needle.isSource() && needle.isSink()) {
												sb.append("source and sink");
											} else if (needle.isSource()) {
												sb.append("source");
											} else if (needle.isSink()) {
												sb.append("sink");
											}
											if (needle.isSource() || needle.isSink()) {
												sb.append(" based on the information in: "
														+ givenGroundTruth.getAbsolutePath());
												Log.msg(sb.toString(), Log.DEBUG_DETAILED);
											}
										}
									}
								}
							}
						}
					} else {
						// Reload
						this.currentMax = Data.getInstance().getMap().get(testcase).size();
						this.currentDone = 1;
						for (final SourceOrSink sourceOrSink : Data.getInstance().getMap().get(testcase)) {
							sourceOrSink.getReference().getApp().setFile(testcase.getApk().getAbsolutePath());
							Data.getInstance().getSourceAndSinkList().add(sourceOrSink);

							if (newMap.get(testcase) != null) {
								newMap.get(testcase).add(sourceOrSink);
							} else {
								final List<SourceOrSink> initialList = new ArrayList<>();
								initialList.add(sourceOrSink);
								newMap.put(testcase, initialList);
							}
							newMapR.put(sourceOrSink, testcase);
							progressDialog.updateProgress(this.currentDone++, this.currentMax, this.totalDone,
									this.totalMax);
						}
					}

					progressDialog.updateProgress(0, 100, this.totalDone++, this.totalMax);
				}

				Data.getInstance().getSourcesAndSinks().addAll(Data.getInstance().getSourceAndSinkList());
				Data.getInstance().setMap(newMap);
				Data.getInstance().setMapR(newMapR);

				progressDialog.updateProgress(100, 100, this.totalMax, this.totalMax);

				sourceAndSinkSelector.refresh();

				sourceAndSinkSelector.loadTaintBench();

				MenuBar.activateBtns();
			}).start();
		} else {
			// Refresh mapR if old SourceAndSink hash is contained
			boolean refreshMapR = false;
			for (final SourceOrSink sos : Data.getInstance().getSourceAndSinkList()) {
				if (Data.getInstance().getMapR().get(sos) == null) {
					refreshMapR = true;
					break;
				}
			}
			if (refreshMapR) {
				final Map<SourceOrSink, Testcase> newMapR = new HashMap<>();
				for (final Testcase testcase : Data.getInstance().getTestcases()) {
					for (final SourceOrSink sos : Data.getInstance().getMap().get(testcase)) {
						if (!newMapR.containsKey(sos)) {
							newMapR.put(sos, testcase);
						}
					}
				}
				Data.getInstance().setMapR(newMapR);
			}

			MenuBar.activateBtns();
		}
	}

	private void restore(SourceOrSink sourceOrSink, List<SourceOrSink> oldList) {
		for (final SourceOrSink testSS : oldList) {
			try {
				String app1 = testSS.getReference().getApp().getFile().replaceAll("\\\\", "/");
				app1 = app1.substring(app1.lastIndexOf("/") + 1);

				String app2 = sourceOrSink.getReference().getApp().getFile().replaceAll("\\\\", "/");
				app2 = app2.substring(app2.lastIndexOf("/") + 1);

				if (app1.equals(app2)) {
					if (EqualsHelper.equals(testSS.getReference().getStatement(),
							sourceOrSink.getReference().getStatement())
							&& testSS.getReference().getMethod().equals(sourceOrSink.getReference().getMethod())
							&& testSS.getReference().getClassname()
									.equals(sourceOrSink.getReference().getClassname())) {
						boolean infoNeeded = false;
						if (testSS.isSource()) {
							sourceOrSink.setSource(true);
							infoNeeded = true;
						}
						if (testSS.isSink()) {
							sourceOrSink.setSink(true);
							infoNeeded = true;
						}
						String newCombine = null;
						if (testSS.getCombine() != null && !testSS.getCombine().equals("")) {
							for (final String combineID : testSS.getCombine().replaceAll(" ", "").split(",")) {
								final long diff = Long.parseLong(combineID) - testSS.getId();
								if (newCombine == null) {
									newCombine = String.valueOf(sourceOrSink.getId() + diff);
								} else {
									newCombine += ", " + (sourceOrSink.getId() + diff);
								}
							}
						}
						if (newCombine != null) {
							sourceOrSink.setCombine(newCombine);
							infoNeeded = true;
						}
						if (infoNeeded) {
							restoreInfo(sourceOrSink);
						}
					}
				}
			} catch (final StringIndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}

	private void restoreInfo(SourceOrSink sourceOrSink) {
		Platform.runLater(() -> {
			final Alert alert = new Alert(AlertType.WARNING);
			final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
			alert.setTitle("Restored Information");
			alert.setHeaderText(
					"Testcases have changed. Recomputation of all sources and sinks (for this testcase) required.");
			alert.setContentText("Please check entry with id " + sourceOrSink.getId() + " for correctness!");
			alert.show();
		});
	}

	private boolean contains(Testcase testcase) {
		if (Data.getInstance().getMap().get(testcase) == null) {
			return false;
		}
		for (final SourceOrSink compare : Data.getInstance().getMap().get(testcase)) {
			String hash = null;
			for (final Hash temp : compare.getReference().getApp().getHashes().getHash()) {
				if (temp.getType().equals(HashHelper.HASH_TYPE_MD5)) {
					hash = temp.getValue();
				}
			}
			if (hash == null) {
				return false;
			}
			if (hash.equals(HashHelper.md5Hash(testcase.getApk()))) {
				return true;
			}
		}
		return false;
	}
}
