package de.foellix.aql.brew.testcaseselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.foellix.aql.Log;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.SootHelper;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.ui.gui.ProgressDialog;
import javafx.stage.Stage;
import soot.SootClass;
import soot.SootMethod;

public class FeatureDetector {
	final de.foellix.aql.helper.tools.FeatureFinder innerFeatureDetector;
	private FileRelocator relocator;

	private int max;
	private int done;

	private static FeatureDetector instance = new FeatureDetector();

	private FeatureDetector() {
		this.innerFeatureDetector = new de.foellix.aql.helper.tools.FeatureFinder();
	}

	public static FeatureDetector getInstance() {
		return instance;
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
				final Collection<SootClass> classes = SootHelper
						.filterDefaultExcludes(SootHelper.getInstance().extract(testcase));
				int classCounter = 0;
				for (final SootClass c : classes) {
					progressDialog.updateProgress(classCounter++, classes.size(), this.done, this.max);
					for (final SootMethod m : c.getMethods()) {
						this.innerFeatureDetector.addFeatures(features, m);
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