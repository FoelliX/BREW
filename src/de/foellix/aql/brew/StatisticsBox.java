package de.foellix.aql.brew;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StatisticsBox extends VBox {
	private final Label topic0, topic1, topic2, topic3, topic4;
	private final TextArea topic5, topic6, topic7;
	private final TitledPane statistics;
	private final TitledPane information;

	final VBox informationOuter;
	final HBox informationInner;

	public StatisticsBox() {
		super(0);

		final HBox statisticsInner = new HBox(20);
		this.topic0 = new Label();
		this.topic1 = new Label();
		this.topic2 = new Label();
		this.topic3 = new Label();
		this.topic4 = new Label();
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

		setInformation("Plese select any item to show more information.");
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
		return Math.min((node.getText().split("\r\n|\r|\n").length + 1) * 18 + 6, 276);
	}

	public void refresh() {
		final int sources = Statistics.getInstance().getSources();
		final int sinks = Statistics.getInstance().getSinks();
		final int pc = Statistics.getInstance().getPositiveCases();
		final int pf = Statistics.getInstance().getPositiveFlows();
		final int nc = Statistics.getInstance().getNegativeCases();
		final int nf = Statistics.getInstance().getNegativeFlows();
		final int tp = Statistics.getInstance().getTruePositives();
		final int fp = Statistics.getInstance().getFalsePositives();
		final int tn = Statistics.getInstance().getTrueNegatives();
		final int fn = Statistics.getInstance().getFalseNegatives();
		final int flowsFound = Statistics.getInstance().getFlowsFound();
		final int flowsFoundPerApp = Statistics.getInstance().getFlowsFoundPerApp();
		final int durationNoTimeouts = Statistics.getInstance().getDuration(false);
		final int durationWithTimeouts = Statistics.getInstance().getDuration(true);
		final int aborts = Statistics.getInstance().getAborts(false);
		final int abortsPerApp = Statistics.getInstance().getAborts(true);

		final float precision = Statistics.getInstance().getPrecision();
		final float recall = Statistics.getInstance().getRecall();
		final float fmeasure = Statistics.getInstance().getFmeasure();

		this.topic0.setText("Testcases: "
				+ (Data.getInstance().getTestcases() != null ? Data.getInstance().getTestcases().size() : 0)
				+ "\nSources: " + sources + "\nSinks: " + sinks);
		this.topic1.setText("Positive cases: " + pc + " (" + pf + ")\nNegative cases: " + nc + " (" + nf + ")");
		this.topic2.setText("Flows Found (per Query): " + flowsFound + " (" + flowsFoundPerApp
				+ ")\nAnalysis time without Timeouts/Crashes: " + durationNoTimeouts + "s ("
				+ Math.round(durationNoTimeouts / 60f) + "m)\nAnalysis time with Timeouts/Crashes: "
				+ durationWithTimeouts + "s (" + Math.round(durationWithTimeouts / 60f)
				+ "m)\nTimeouts/Crashes (per App): " + aborts + " (" + abortsPerApp + ")");
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
		Platform.runLater(() -> {
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
			if ((information2 != null && !information2.isEmpty())
					|| (information1 != null && !information1.isEmpty())) {
				this.informationOuter.getChildren().add(this.informationInner);
			}
			this.topic7.setText(information3);
			if (information3 != null && !information3.isEmpty()) {
				this.informationOuter.getChildren().add(this.topic7);
			}
		});
	}
}