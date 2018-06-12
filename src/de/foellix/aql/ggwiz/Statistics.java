package de.foellix.aql.ggwiz;

import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.tpfpselector.TPFP;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Statistics extends VBox {
	private final Label topic0, topic1, topic2, topic3, topic4, topic5, topic6, topic7;
	private final TitledPane statistics;
	private final TitledPane information;

	private static Statistics instance = new Statistics();

	private Statistics() {
		super(0);

		final HBox statisticsInner = new HBox(50);
		this.topic0 = new Label();
		this.topic1 = new Label();
		this.topic2 = new Label();
		this.topic3 = new Label();
		this.topic4 = new Label();
		refresh();
		statisticsInner.getChildren().addAll(this.topic0, this.topic1, this.topic2, this.topic3, this.topic4);
		this.statistics = new TitledPane("Statistics", statisticsInner);

		final VBox informationOuter = new VBox(10);
		final HBox informationInner = new HBox(50);
		this.topic5 = new Label();
		this.topic6 = new Label();
		this.topic7 = new Label();
		informationInner.getChildren().addAll(this.topic5, this.topic6);
		informationOuter.getChildren().addAll(informationInner, this.topic7);
		this.information = new TitledPane("Information", informationOuter);
		this.information.setExpanded(false);

		this.getChildren().addAll(this.statistics, this.information);
	}

	public static Statistics getInstance() {
		return instance;
	}

	public void refresh() {
		int sources = 0;
		int sinks = 0;
		if (Data.getInstance().getSourcesAndSinks() != null) {
			for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
				if (item.isSource()) {
					sources++;
				}
				if (item.isSink()) {
					sinks++;
				}
			}
		}

		int pc = 0;
		int nc = 0;
		int tp = 0;
		int fp = 0;
		int tn = 0;
		int fn = 0;
		if (Data.getInstance().getTPFPs() != null) {
			for (final TPFP item : Data.getInstance().getTPFPs()) {
				if (item.isTruepositive()) {
					pc++;
					if (item.getStatus() == TPFP.SUCCESSFUL) {
						tp++;
					} else {
						fn++;
					}
				}
				if (item.isFalsepositive()) {
					nc++;
					if (item.getStatus() == TPFP.SUCCESSFUL) {
						tn++;
					} else {
						fp++;
					}
				}
			}
		}

		float precision = Float.valueOf(tp).floatValue()
				/ (Float.valueOf(tp).floatValue() + Float.valueOf(fp).floatValue());
		float recall = Float.valueOf(tp).floatValue()
				/ (Float.valueOf(tp).floatValue() + Float.valueOf(fn).floatValue());
		float fmeasure = 2f * ((precision * recall) / (precision + recall));
		precision = Math.round(precision * 1000f) / 1000f;
		recall = Math.round(recall * 1000f) / 1000f;
		fmeasure = Math.round(fmeasure * 1000f) / 1000f;

		this.topic0.setText("Testcases: "
				+ (Data.getInstance().getTestcases() != null ? Data.getInstance().getTestcases().size() : 0));
		this.topic1.setText("Sources: " + sources + "\nSinks: " + sinks);
		this.topic2.setText("Positive cases: " + pc + "\nNegative cases: " + nc);
		this.topic3.setText("True Positive: " + tp + "\nFalse Positive: " + fp + "\nTrue Negative: " + tn
				+ "\nFalse Negative: " + fn);
		this.topic4.setText("Precision: " + precision + "\nRecall: " + recall + "\nF-Measure: " + fmeasure);
	}

	public void setInformation(String information) {
		setInformation(information, "", "");
	}

	public void setInformation(String information1, String information2) {
		setInformation(information1, information2, "");
	}

	public void setInformation(String information1, String information2, String information3) {
		this.topic5.setText(information1);
		this.topic6.setText(information2);
		this.topic7.setText(information3);
	}
}