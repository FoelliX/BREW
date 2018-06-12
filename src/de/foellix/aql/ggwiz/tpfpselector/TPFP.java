package de.foellix.aql.ggwiz.tpfpselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.Helper;

public class TPFP implements Serializable {
	private static final long serialVersionUID = -8835869835882399230L;

	public static final int DEFAULT = 0;
	public static final int SUCCESSFUL = 1;
	public static final int FAILED = 2;

	private int id;
	private final SourceOrSink from, to;
	private boolean truepositive, falsepositive;
	private int status;
	private boolean aborted;
	private long started, ended;

	public TPFP(SourceOrSink from, SourceOrSink to) {
		this.id = Data.getInstance().getTPFPId();
		this.from = from;
		this.to = to;

		// Initialize
		this.truepositive = true;
		this.falsepositive = false;
		this.status = DEFAULT;
		this.aborted = false;
		this.started = 0;
		this.ended = 0;
	}

	@Override
	public boolean equals(Object tpfp) {
		if (tpfp instanceof TPFP) {
			return EqualsHelper.equals(this.from.getReference().getStatement(),
					((TPFP) tpfp).getFrom().getReference().getStatement())
					&& EqualsHelper.equals(this.to.getReference().getStatement(),
							((TPFP) tpfp).getTo().getReference().getStatement())
					&& EqualsHelper.equals(this.from.getReference(), ((TPFP) tpfp).getFrom().getReference())
					&& EqualsHelper.equals(this.to.getReference(), ((TPFP) tpfp).getTo().getReference())
					&& ((this.from.getCombine() == null && ((TPFP) tpfp).getFrom().getCombine() == null)
							|| this.from.getCombine().equals(((TPFP) tpfp).getFrom().getCombine()))
					&& ((this.to.getCombine() == null && ((TPFP) tpfp).getTo().getCombine() == null)
							|| this.to.getCombine().equals(((TPFP) tpfp).getTo().getCombine()));
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
				if (!idStr.equals("")) {
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
		if (this.to.getCombine() != null) {
			for (final String idStr : this.to.getCombine().replace(" ", "").split(",")) {
				if (!idStr.equals("")) {
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
				source.setType(KeywordsAndConstants.REFERENCE_TYPE_FROM);
				sink.setType(KeywordsAndConstants.REFERENCE_TYPE_TO);
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

	public String getTestcase() {
		final int maxlength = 30;
		String name = Data.getInstance().getMapR().get(this.from).getName();
		if (name.length() > maxlength) {
			name = "..." + name.substring(name.length() - maxlength);
		}
		return Data.getInstance().getMapR().get(this.from).getId() + ": " + name;
	}

	public String getTestcaseComplete() {
		return Data.getInstance().getMapR().get(this.from).getId() + ": "
				+ Data.getInstance().getMapR().get(this.from).getName() + " -> "
				+ Data.getInstance().getMapR().get(this.to).getId() + ": "
				+ Data.getInstance().getMapR().get(this.to).getName();
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
		return this.falsepositive;
	}

	public void setFalsepositive(boolean falsepositive) {
		this.falsepositive = falsepositive;
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

	public int getDuration() {
		return (int) ((this.ended - this.started) / 1000);
	}
}
