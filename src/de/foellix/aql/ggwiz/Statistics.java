package de.foellix.aql.ggwiz;

import java.util.List;

import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.tpfpselector.TPFP;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Statistics extends VBox {
	private final Label topic0, topic1, topic2, topic3, topic4;
	private final TextArea topic5, topic6, topic7;
	private final TitledPane statistics;
	private final TitledPane information;

	final VBox informationOuter;
	final HBox informationInner;

	private static Statistics instance = new Statistics();

	private Statistics() {
		super(0);

		final HBox statisticsInner = new HBox(20);
		this.topic0 = new Label();
		this.topic1 = new Label();
		this.topic2 = new Label();
		this.topic3 = new Label();
		this.topic4 = new Label();
		refresh();
		statisticsInner.getChildren().addAll(this.topic0, this.topic1, this.topic2, this.topic3, this.topic4);
		this.statistics = new TitledPane("Statistics", statisticsInner);

		this.informationOuter = new VBox(5);
		this.informationInner = new HBox(5);
		this.topic5 = new TextArea();
		this.topic6 = new TextArea();
		this.topic7 = new TextArea();
		this.informationInner.getChildren().addAll(this.topic5, this.topic6);
		this.informationOuter.getChildren().addAll(this.informationInner, this.topic7);
		this.information = new TitledPane("Information", this.informationOuter);
		this.information.setExpanded(false);

		this.getChildren().addAll(this.statistics, this.information);

		initElements(new Control[] { this.topic5, this.topic6, this.topic7 });
	}

	public static Statistics getInstance() {
		return instance;
	}

	private void initElements(Control[] nodes) {
		for (final Control node : nodes) {
			node.setPrefWidth(Integer.MAX_VALUE);
			if (node instanceof TextInputControl) {
				((TextInputControl) node).setEditable(false);
				if (node instanceof TextArea) {
					((TextArea) node).setWrapText(true);

					final SimpleIntegerProperty count = new SimpleIntegerProperty(getHeight((TextArea) node));

					((TextArea) node).prefHeightProperty().bindBidirectional(count);
					((TextArea) node).minHeightProperty().bindBidirectional(count);
					((TextArea) node).textProperty().addListener((ov, oldVal, newVal) -> {
						count.setValue(getHeight((TextArea) node));
					});
				}
			}
		}
	}

	private int getHeight(TextArea node) {
		return (node.getText().split("\r\n|\r|\n").length + 1) * 18 + 6;
	}

	public void refresh() {
		refresh(Data.getInstance().getTPFPs());
	}

	public void refresh(List<TPFP> tpfps) {
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
		if (tpfps != null) {
			for (final TPFP item : tpfps) {
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
		this.informationInner.getChildren().clear();
		this.informationOuter.getChildren().clear();
		this.topic5.setText(information1);
		if (information1 != null && !information1.isEmpty()) {
			this.informationInner.getChildren().add(this.topic5);
		}
		this.topic6.setText(information2);
		if (information2 != null && !information2.isEmpty()) {
			this.informationInner.getChildren().add(this.topic6);
		}
		if ((information2 != null && !information2.isEmpty()) || (information1 != null && !information1.isEmpty())) {
			this.informationOuter.getChildren().add(this.informationInner);
		}
		this.topic7.setText(information3);
		if (information3 != null && !information3.isEmpty()) {
			this.informationOuter.getChildren().add(this.topic7);
		}
	}
}