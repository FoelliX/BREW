package de.foellix.aql.brew;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.tpfpselector.TPFP;
import javafx.application.Platform;

public class Statistics {
	private StatisticsBox statisticsBox;

	private int sources = 0;
	private int sinks = 0;

	private int pc = 0;
	private int pf = 0;
	private int nc = 0;
	private int nf = 0;
	private int tp = 0;
	private int fp = 0;
	private int tn = 0;
	private int fn = 0;
	private int flowsFound = 0;
	private int flowsFoundPerApp = 0;
	private int durationNoTimeouts = 0;
	private int durationWithTimeouts = 0;
	private int aborts = 0;
	private int abortsPerApp = 0;

	private float precision = 0;
	private float recall = 0;
	private float fmeasure = 0;

	private static Statistics instance = new Statistics();

	private Statistics() {
		if (!BREW.getNoGui()) {
			this.statisticsBox = new StatisticsBox();
		}

		refresh();
	}

	public static Statistics getInstance() {
		return instance;
	}

	public void refresh() {
		refresh(Data.getInstance().getTPFPs());
	}

	public void refresh(List<TPFP> tpfps) {
		this.sources = 0;
		this.sinks = 0;
		if (Data.getInstance().getSourcesAndSinks() != null) {
			for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
				if (item.isSource()) {
					this.sources++;
				}
				if (item.isSink()) {
					this.sinks++;
				}
			}
		}

		this.pc = 0;
		this.pf = 0;
		this.nc = 0;
		this.nf = 0;
		this.tp = 0;
		this.fp = 0;
		this.tn = 0;
		this.fn = 0;
		this.flowsFound = 0;
		this.flowsFoundPerApp = 0;
		this.durationNoTimeouts = 0;
		this.durationWithTimeouts = 0;
		this.aborts = 0;
		this.abortsPerApp = 0;
		if (tpfps != null) {
			final Set<String> countedFlows = new HashSet<>();
			final Set<String> countedTimes = new HashSet<>();
			final Set<String> countedCrashes = new HashSet<>();
			for (final TPFP item : tpfps) {
				if (item.isTruepositive()) {
					this.pc++;
					// this.pf += item.toAnswer().getFlows().getFlow().size();
					this.pf += countFlows(item);
					if (item.getStatus() == TPFP.SUCCESSFUL) {
						this.tp++;
					} else {
						this.fn++;
					}
				}
				if (item.isFalsepositive()) {
					this.nc++;
					// this.nf += item.toAnswer().getFlows().getFlow().size();
					this.nf += countFlows(item);
					if (item.getStatus() == TPFP.SUCCESSFUL) {
						this.tn++;
					} else {
						this.fp++;
					}
				}
				final String identifier = item.getStarted() + "#" + item.getEnded() + "#"
						+ item.getFrom().getReference().getApp().getFile() + "#"
						+ item.getTo().getReference().getApp().getFile();

				// Count flows
				if (!item.isAborted() && item.getActualAnswer() != null && item.getActualAnswer().getFlows() != null
						&& !item.getActualAnswer().getFlows().getFlow().isEmpty()) {
					if (!countedFlows.contains(identifier)) {
						this.flowsFoundPerApp += item.getActualAnswer().getFlows().getFlow().size();
					}
					this.flowsFound += item.getActualAnswer().getFlows().getFlow().size();
				}
				countedFlows.add(identifier);

				// Count time and aborts
				if (!countedTimes.contains(identifier)) {
					countedTimes.add(identifier);
					if (!item.isAborted()) {
						this.durationNoTimeouts += item.getDuration();
					} else if (item.isAborted()) {
						final String identifier2 = item.getFrom().getReference().getApp().getFile() + "#"
								+ item.getTo().getReference().getApp().getFile();
						if (!countedCrashes.contains(identifier2)) {
							countedCrashes.add(identifier2);
							this.abortsPerApp++;
						}
						this.aborts++;
					}
					this.durationWithTimeouts += item.getDuration();
				} else if (item.isAborted()) {
					this.aborts++;
				}
			}
		}

		this.precision = Float.valueOf(this.tp).floatValue()
				/ (Float.valueOf(this.tp).floatValue() + Float.valueOf(this.fp).floatValue());
		this.recall = Float.valueOf(this.tp).floatValue()
				/ (Float.valueOf(this.tp).floatValue() + Float.valueOf(this.fn).floatValue());
		this.fmeasure = 2f * ((this.precision * this.recall) / (this.precision + this.recall));
		this.precision = Math.round(this.precision * 1000f) / 1000f;
		this.recall = Math.round(this.recall * 1000f) / 1000f;
		this.fmeasure = Math.round(this.fmeasure * 1000f) / 1000f;

		if (instance != null && !BREW.getNoGui()) {
			Platform.runLater(() -> {
				this.statisticsBox.refresh();
			});
		}
	}

	private int countFlows(TPFP item) {
		int x = 1;
		if (item.getFrom().getCombine() != null && !item.getFrom().getCombine().isEmpty()) {
			x = item.getFrom().getCombine().split(",").length + 1;
		}
		int y = 1;
		if (item.getTo().getCombine() != null && !item.getTo().getCombine().isEmpty()) {
			y = item.getTo().getCombine().split(",").length + 1;
		}
		return x * y;
	}

	public String getStatisticsAsString(boolean breaklines) {
		final String separator = (breaklines ? "\n" : " -> ");
		return "Testcases: "
				+ (Data.getInstance().getTestcases() != null ? Data.getInstance().getTestcases().size() : 0) + separator
				+ "Sources: " + this.sources + ", Sinks: " + this.sinks + separator + "Positive cases: " + this.pc
				+ " (" + this.pf + "), Negative cases: " + this.nc + " (" + this.nf + ")" + separator
				+ "Flows found (per Query): " + this.flowsFound + " (" + this.flowsFoundPerApp
				+ "), Analysis time without Timeouts/Crashes: " + this.durationNoTimeouts + "s ("
				+ Math.round(this.durationNoTimeouts / 60f) + "m), Analysis time with Timeouts/Crashes: "
				+ this.durationWithTimeouts + "s (" + Math.round(this.durationWithTimeouts / 60f)
				+ "m), Timeouts/Crashes (per App): " + this.aborts + " (" + this.abortsPerApp + ")" + separator
				+ "True Positive: " + this.tp + ", False Positive: " + this.fp + ", True Negative: " + this.tn
				+ ", False Negative: " + this.fn + separator + "Precision: " + this.precision + ", Recall: "
				+ this.recall + ", F-Measure: " + this.fmeasure;
	}

	public StatisticsBox getStatisticsBox() {
		return this.statisticsBox;
	}

	public int getSources() {
		return this.sources;
	}

	public int getSinks() {
		return this.sinks;
	}

	public int getPositiveCases() {
		return this.pc;
	}

	public int getPositiveFlows() {
		return this.pf;
	}

	public int getNegativeCases() {
		return this.nc;
	}

	public int getNegativeFlows() {
		return this.nf;
	}

	public int getFlowsFound() {
		return this.flowsFound;
	}

	public int getFlowsFoundPerApp() {
		return this.flowsFoundPerApp;
	}

	public int getDuration(boolean includeTimeouts) {
		if (includeTimeouts) {
			return this.durationWithTimeouts;
		} else {
			return this.durationNoTimeouts;
		}
	}

	public int getAborts(boolean perApp) {
		if (perApp) {
			return this.abortsPerApp;
		} else {
			return this.aborts;
		}
	}

	public int getTruePositives() {
		return this.tp;
	}

	public int getFalsePositives() {
		return this.fp;
	}

	public int getTrueNegatives() {
		return this.tn;
	}

	public int getFalseNegatives() {
		return this.fn;
	}

	public float getPrecision() {
		return this.precision;
	}

	public float getRecall() {
		return this.recall;
	}

	public float getFmeasure() {
		return this.fmeasure;
	}
}