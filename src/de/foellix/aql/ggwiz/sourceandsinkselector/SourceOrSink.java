package de.foellix.aql.ggwiz.sourceandsinkselector;

import java.io.Serializable;

import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.EqualsHelper;

public class SourceOrSink implements Serializable {
	private static final long serialVersionUID = -5434428962258152698L;

	private int id;
	private boolean source;
	private boolean sink;
	private final Reference reference;
	private String combine;

	public SourceOrSink(boolean source, boolean sink, Reference reference) {
		this.id = Data.getInstance().getSourceOrSinkId();
		this.source = source;
		this.sink = sink;
		this.reference = reference;

		// Preselect
		if (reference.getStatement().getStatementgeneric().contains("startActivity")) {
			sink = true;
		}
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SourceOrSink) {
			final SourceOrSink temp = (SourceOrSink) obj;
			if (EqualsHelper.equals(this.reference.getStatement(), temp.getReference().getStatement())
					&& EqualsHelper.equals(this.reference, temp.getReference())) {
				return true;
			}
		}
		return false;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public String getTestcase() {
		try {
			final int maxlength = 30;
			String name = Data.getInstance().getMapR().get(this).getName();
			if (name.length() > maxlength) {
				name = "..." + name.substring(name.length() - maxlength);
			}
			return Data.getInstance().getMapR().get(this).getId() + ": " + name;
		} catch (final NullPointerException e) {
			return "";
		}
	}

	public String getTestcaseComplete() {
		return Data.getInstance().getMapR().get(this).getId() + ": " + Data.getInstance().getMapR().get(this).getName();
	}

	public boolean isSource() {
		return this.source;
	}

	public void setSource(boolean source) {
		this.source = source;
	}

	public boolean isSink() {
		return this.sink;
	}

	public void setSink(boolean sink) {
		this.sink = sink;
	}

	public Reference getReference() {
		return this.reference;
	}

	public String getStatement() {
		return this.reference.getStatement().getStatementgeneric();
	}

	public String getCombine() {
		return this.combine;
	}

	public void setCombine(String combine) {
		this.combine = combine;
	}
}
