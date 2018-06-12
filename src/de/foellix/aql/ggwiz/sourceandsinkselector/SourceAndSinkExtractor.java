package de.foellix.aql.ggwiz.sourceandsinkselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Hash;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.SootHelper;
import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;
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
		for (final Testcase testcase : Data.getInstance().getTestcases()) {
			if (!testcase.getApk().exists()) {
				testcase.setApk(this.relocator.relocateFile(testcase.getApk()));
			}
		}

		final List<SourceOrSink> oldList = new ArrayList<>();
		oldList.addAll(Data.getInstance().getSourcesAndSinks());

		Data.getInstance().getSourcesAndSinks().clear();
		final Map<Testcase, List<SourceOrSink>> newMap = new HashMap<>();
		final Map<SourceOrSink, Testcase> newMapR = new HashMap<>();

		final ProgressDialog progressDialog = new ProgressDialog();
		this.totalMax = Math.max(Data.getInstance().getTestcases().size(), 1);
		this.totalDone = 0;
		progressDialog.updateProgress(0, 100, this.totalDone, this.totalMax);

		new Thread(() -> {
			for (final Testcase testcase : Data.getInstance().getTestcases()) {
				Reference reference = null;
				if (!contains(testcase)) {
					// Extract
					Log.msg("Determining sources & sinks for: " + testcase.getId() + ": " + testcase.getName(),
							Log.NORMAL);
					final Collection<SootClass> classes = SootHelper.getInstance().extract(testcase);
					int classCounter = 0;
					for (final SootClass c : classes) {
						progressDialog.updateProgress(classCounter++, classes.size(), this.totalDone, this.totalMax);
						if (c.isConcrete()) {
							for (final SootMethod m : c.getMethods()) {
								if (m.isConcrete()) {
									final Body b = m.retrieveActiveBody();
									final List<SourceOrSink> checkList = new ArrayList<>();
									for (final Unit u : b.getUnits()) {
										if (u.toString().contains("invoke")
												&& !u.toString().contains("android.content.Intent: void <init>()")
												&& !u.toString().contains("android.content.IntentFilter: void <init>()")
												&& !u.toString()
														.contains("android.content.BroadcastReceiver: void <init>()")
												&& !u.toString()
														.contains("android.content.ContentProvider: void <init>()")
												&& !u.toString().contains("android.app.Activity: void <init>()")
												&& !u.toString().contains("android.app.Service: void <init>()")
												&& !u.toString().contains("java.lang.Object: void <init>()")) {
											reference = new Reference();
											reference.setApp(Helper.createApp(testcase.getApk()));
											reference.setClassname(c.getName());
											reference.setMethod(m.getSignature());
											reference.setStatement(Helper.fromStatementString(u.toString()));

											final SourceOrSink sourceOrSink = new SourceOrSink(false, false, reference);
											if (oldList != null && !oldList.isEmpty()) {
												restore(sourceOrSink, oldList);
											}
											if (!checkList.contains(sourceOrSink)) {
												Data.getInstance().getSourcesAndSinks().add(sourceOrSink);
												checkList.add(sourceOrSink);

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
					}
				} else {
					// Reload
					this.currentMax = Data.getInstance().getMap().get(testcase).size();
					this.currentDone = 1;
					// final List<SourceOrSink> checkList = new ArrayList<>();
					for (final SourceOrSink sourceOrSink : Data.getInstance().getMap().get(testcase)) {
						sourceOrSink.getReference().getApp().setFile(testcase.getApk().getAbsolutePath());
						// if (!checkList.contains(sourceOrSink)) {
						Data.getInstance().getSourcesAndSinks().add(sourceOrSink);
						// checkList.add(sourceOrSink);

						if (newMap.get(testcase) != null) {
							newMap.get(testcase).add(sourceOrSink);
						} else {
							final List<SourceOrSink> initialList = new ArrayList<>();
							initialList.add(sourceOrSink);
							newMap.put(testcase, initialList);
						}
						newMapR.put(sourceOrSink, testcase);
						// }
						progressDialog.updateProgress(this.currentDone++, this.currentMax, this.totalDone,
								this.totalMax);
					}
				}

				progressDialog.updateProgress(0, 100, this.totalDone++, this.totalMax);
			}

			Data.getInstance().setMap(newMap);
			Data.getInstance().setMapR(newMapR);

			progressDialog.updateProgress(0, 100, this.totalMax, this.totalMax);

			sourceAndSinkSelector.refresh();
		}).start();
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
								final int diff = Integer.parseInt(combineID) - testSS.getId();
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
				if (temp.getType().equals(KeywordsAndConstants.HASH_TYPE_MD5)) {
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
