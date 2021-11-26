package de.foellix.aql.brew.tpfpselector;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.Exporter;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.system.task.gui.TaskTreeSnapshot;

public class TPFP implements Serializable {
	private static final long serialVersionUID = -8835869835882399230L;

	public static final int DEFAULT = 0;
	public static final int SUCCESSFUL = 1;
	public static final int FAILED = 2;

	private int id;
	private final SourceOrSink from, to;
	private boolean truepositive;
	private Map<String, String> attributes;

	private Answer actual;
	private transient TaskTreeSnapshot TaskTreeSnapshot;
	private int status;
	private boolean aborted;
	private long started, ended;

	public TPFP(SourceOrSink from, SourceOrSink to) {
		this.id = Data.getInstance().getTPFPId();
		this.from = from;
		this.to = to;
		this.truepositive = true;

		// Initialize
		reset();
	}

	public void reset() {
		this.actual = null;
		this.status = DEFAULT;
		this.TaskTreeSnapshot = null;
		this.aborted = false;
		this.started = 0;
		this.ended = 0;
	}

	@Override
	public boolean equals(Object tpfp) {
		if (tpfp instanceof TPFP) {
			final TPFP tpfpCasted = (TPFP) tpfp;
			return this.from.equals(tpfpCasted.getFrom()) && this.to.equals(tpfpCasted.getTo())
					&& ((this.from.getCombine() == null && tpfpCasted.getFrom().getCombine() == null)
							|| this.from.getCombine().equals(tpfpCasted.getFrom().getCombine()))
					&& ((this.to.getCombine() == null && tpfpCasted.getTo().getCombine() == null)
							|| this.to.getCombine().equals(tpfpCasted.getTo().getCombine()));
		} else {
			return false;
		}
	}

	public boolean equalsSloppy(Object tpfp) {
		if (tpfp instanceof TPFP) {
			final TPFP tpfpCasted = (TPFP) tpfp;
			final EqualsOptions sloppy = new EqualsOptions().setOption(EqualsOptions.IGNORE_APP, true);
			return EqualsHelper.equals(this.from.getReference(), tpfpCasted.getFrom().getReference(), sloppy)
					&& EqualsHelper.equals(this.to.getReference(), tpfpCasted.getTo().getReference(), sloppy);
		} else {
			return false;
		}
	}

	public Answer toAnswer() {
		final Answer answer = new Answer();
		final Flows flows = new Flows();

		// combine
		final List<Reference> fromList = new ArrayList<>();
		final List<Reference> toList = new ArrayList<>();
		fromList.add(this.from.getReference());
		toList.add(this.to.getReference());
		if (this.from.getCombine() != null) {
			for (final String idStr : this.from.getCombine().replace(" ", "").split(",")) {
				if (!idStr.isEmpty()) {
					final int id = Integer.valueOf(idStr).intValue();
					for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
						if (id == item.getId()) {
							fromList.add(item.getReference());
							break;
						}
					}
				}
			}
		}
		if (this.to.getCombine() != null && !this.to.getCombine().isEmpty()) {
			for (final String idStr : this.to.getCombine().replace(" ", "").split(",")) {
				if (idStr != null && !idStr.equals("null") && !idStr.isEmpty()) {
					final int id = Integer.valueOf(idStr).intValue();
					for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
						if (id == item.getId()) {
							toList.add(item.getReference());
							break;
						}
					}
				}
			}
		}
		for (final Reference source : fromList) {
			for (Reference sink : toList) {
				if (source == sink) {
					sink = Helper.copy(source);
				}
				source.setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_FROM);
				sink.setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_TO);
				final Flow flow = new Flow();
				flow.getReference().add(source);
				flow.getReference().add(sink);
				flows.getFlow().add(flow);
			}
		}
		answer.setFlows(flows);

		return answer;
	}

	public String getCase() {
		String caseStr = "";
		if (this.to.getReference().getClassname().contains("IsolateActivity")) {
			if (!this.from.getReference().getClassname().contains("IsolateActivity")) {
				caseStr += "* ";
			}
		}
		caseStr += Helper.cut(this.from.getReference().getStatement().getStatementgeneric(), " ", Helper.OCCURENCE_LAST)
				+ (this.from.getCombine() != null
						? " (" + this.from.getCombine().replace(" ", "").split(",").length + ") -> "
						: " -> ")
				+ Helper.cut(this.to.getReference().getStatement().getStatementgeneric(), " ", Helper.OCCURENCE_LAST)
				+ (this.to.getCombine() != null ? " (" + this.to.getCombine().replace(" ", "").split(",").length + ")"
						: "");
		return caseStr;
	}

	public Testcase getTestcase() {
		for (final Testcase tc : Data.getInstance().getTestcases()) {
			if (tc.getApk().equals(new File(this.from.getReference().getApp().getFile()))) {
				return tc;
			}
		}
		return null;
	}

	public String getBenchmarkcase() {
		final int maxlength = 30;
		String name = Data.getInstance().getMapR().get(this.from).getName();
		if (name.length() > maxlength) {
			name = "..." + name.substring(name.length() - maxlength);
		}
		return Data.getInstance().getMapR().get(this.from).getId() + ": " + name;
	}

	public String getBenchmarkcaseComplete() {
		if (Data.getInstance().getMapR().get(this.from) == null || Data.getInstance().getMapR().get(this.to) == null) {
			return null;
		} else {
			return Data.getInstance().getMapR().get(this.from).getId() + ": "
					+ Data.getInstance().getMapR().get(this.from).getName() + " -> "
					+ Data.getInstance().getMapR().get(this.to).getId() + ": "
					+ Data.getInstance().getMapR().get(this.to).getName();
		}
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isTruepositive() {
		return this.truepositive;
	}

	public void setTruepositive(boolean truepositive) {
		this.truepositive = truepositive;
	}

	public boolean isFalsepositive() {
		return !this.truepositive;
	}

	public void setFalsepositive(boolean falsepositive) {
		this.truepositive = !falsepositive;
	}

	public int getId() {
		return this.id;
	}

	public SourceOrSink getFrom() {
		return this.from;
	}

	public SourceOrSink getTo() {
		return this.to;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public TaskTreeSnapshot getTaskTreeSnapshot() {
		return this.TaskTreeSnapshot;
	}

	public void setTaskTreeSnapshot(TaskTreeSnapshot taskTreeSnapshot) {
		this.TaskTreeSnapshot = taskTreeSnapshot;
	}

	public boolean isAborted() {
		return this.aborted;
	}

	public void setAborted(boolean aborted) {
		this.aborted = aborted;
	}

	public long getStarted() {
		return this.started;
	}

	public void setStarted(long started) {
		if (this.started == 0) {
			this.started = started;
		}
	}

	public long getEnded() {
		return this.ended;
	}

	public void setEnded(long ended) {
		if (this.ended == 0 && this.started != 0) {
			this.ended = ended;
			if (ended < 0) {
				this.started = 0;
			}
		}
	}

	public String getFlowsFound() {
		if (this.actual == null) {
			return "n/a";
		} else if (this.actual.getFlows() == null || this.actual.getFlows().getFlow().isEmpty()) {
			return "0";
		} else {
			return String.valueOf(this.actual.getFlows().getFlow().size());
		}
	}

	public int getDuration() {
		if (this.ended != 0) {
			return (int) ((this.ended - this.started) / 1000);
		} else {
			return 0;
		}
	}

	public Answer getActualAnswer() {
		return this.actual;
	}

	public void setActualAnswer(Answer actual) {
		this.actual = actual;
	}

	public String getAttributesAsString() {
		final StringBuilder sbExpected = new StringBuilder();
		for (final String key : this.attributes.keySet()) {
			if (sbExpected.length() > 0) {
				sbExpected.append(", ");
			}
			sbExpected.append(key + "=" + this.attributes.get(key));
		}

		final StringBuilder sbActual = new StringBuilder();
		if (this.getActualAnswer() != null && this.getActualAnswer().getFlows() != null
				&& !this.getActualAnswer().getFlows().getFlow().isEmpty()) {
			final Answer answerWithAttributes = Exporter.assignAttributes(this, getActualAnswer(),
					String.valueOf(this.id));
			final List<Flow> flows = Runner.getContained(answerWithAttributes, toAnswer());
			if (!flows.isEmpty()) {
				for (final Flow flow : flows) {
					if (flow.getAttributes() != null && !flow.getAttributes().getAttribute().isEmpty()) {
						for (final Attribute attribute : flow.getAttributes().getAttribute()) {
							if (sbActual.length() > 0) {
								sbActual.append(", ");
							}
							sbActual.append(attribute.getName() + "=" + attribute.getValue());
						}
					}
				}
			}
		}

		return (sbExpected.length() > 0 ? "Expected [" + sbExpected.toString() + "]\n" : "")
				+ (sbActual.length() > 0 ? "Actual [" + sbActual + "]" : "");
	}

	public Map<String, String> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new HashMap<>();
		}
		return this.attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return this.id + ") " + Helper.toString(this.from.getReference()) + " -> "
				+ Helper.toString(this.to.getReference());
	}
}