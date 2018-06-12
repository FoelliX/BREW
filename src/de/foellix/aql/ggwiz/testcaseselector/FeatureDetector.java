package de.foellix.aql.ggwiz.testcaseselector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.SootHelper;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.ui.gui.ProgressDialog;
import javafx.stage.Stage;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class FeatureDetector {
	private static final String FEATURES_FILE = "data/Features.txt";

	private FileRelocator relocator;

	private int max;
	private int done;

	private final Map<String, String> featureMap;

	private static FeatureDetector instance = new FeatureDetector();

	private FeatureDetector() {
		this.featureMap = new FeatureMap<>();
		load();
	}

	public static FeatureDetector getInstance() {
		return instance;
	}

	private void load() {
		// Read SuSi file
		final Path featuresFile = new File(FEATURES_FILE).toPath();
		try {
			final List<String> lines = Files.readAllLines(featuresFile, Charset.forName("UTF-8"));
			final List<String> check = new ArrayList<>();
			for (final String line : lines) {
				if (!line.startsWith("%") && line.contains(" -> ")) {
					final String[] parts = line.split(" -> ");
					final String stm = parts[0];
					String feature = parts[1];
					for (final String item : check) {
						if (feature.equals(item)) {
							feature = item;
							break;
						}
					}
					if (!check.contains(feature)) {
						check.add(feature);
					}
					this.featureMap.put(stm, feature);
				}
			}
			Log.msg("Loaded " + this.featureMap.keySet().size() + " statements for " + check.size()
					+ " distinct features.", Log.DEBUG_DETAILED);
		} catch (final IOException e) {
			Log.msg("Could not load features file (" + featuresFile.toString() + ").", Log.DEBUG);
		}
	}

	public void apply(Stage stage, TestCaseSelector testCaseSelector) {
		// Relocate files if not traceable
		if (this.relocator == null) {
			this.relocator = new FileRelocator(stage);
		}
		for (final Testcase testcase : Data.getInstance().getTestcases()) {
			if (!testcase.getApk().exists()) {
				testcase.setApk(this.relocator.relocateFile(testcase.getApk()));
			}
		}

		// Extract features
		final ProgressDialog progressDialog = new ProgressDialog();
		this.max = Math.max(Data.getInstance().getTestcases().size(), 1);
		this.done = 0;
		progressDialog.updateProgress(0, 100, this.done, this.max);

		new Thread(() -> {
			for (final Testcase testcase : Data.getInstance().getTestcases()) {
				final List<String> features = new ArrayList<>();
				Log.msg("Detecting features in: " + testcase.getId() + ": " + testcase.getName(), Log.NORMAL);
				final Collection<SootClass> classes = SootHelper.getInstance().extract(testcase);
				int classCounter = 0;
				for (final SootClass c : classes) {
					progressDialog.updateProgress(classCounter++, classes.size(), this.done, this.max);
					if (c.isConcrete()) {
						for (final SootMethod m : c.getMethods()) {
							if (m.isConcrete()) {
								final Body b = m.retrieveActiveBody();
								for (final Unit u : b.getUnits()) {
									if (u.toString().contains("invoke")
											&& !u.toString().contains("android.content.Intent: void <init>()")
											&& !u.toString().contains("android.content.IntentFilter: void <init>()")
											&& !u.toString()
													.contains("android.content.BroadcastReceiver: void <init>()")
											&& !u.toString().contains("android.content.ContentProvider: void <init>()")
											&& !u.toString().contains("android.app.Activity: void <init>()")
											&& !u.toString().contains("android.app.Service: void <init>()")
											&& !u.toString().contains("java.lang.Object: void <init>()")) {
										if (this.featureMap.get(u.toString()) != null) {
											final List<String> currentFeatures = Arrays.asList(
													this.featureMap.get(u.toString()).replaceAll(" ", "").split(","));
											if (currentFeatures != null) {
												for (final String feature : currentFeatures) {
													if (!features.contains(feature)) {
														features.add(feature);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				Collections.sort(features);
				testcase.setFeatures(features);

				progressDialog.updateProgress(0, 100, this.done++, this.max);
			}

			progressDialog.updateProgress(0, 100, this.max, this.max);

			testCaseSelector.refresh();
		}).start();
	}
}
