package de.foellix.aql.brew.sourceandsinkselector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.foellix.aql.brew.Data;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;

public class SourceOrSink implements Serializable {
	private static final EqualsOptions EQUALS_OPTIONS = new EqualsOptions()
			.setOption(EqualsOptions.CONSIDER_LINENUMBER, true).setOption(EqualsOptions.PRECISELY_REFERENCE, true);

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
	}

	@Override
	public int hashCode() {
		return Helper.toRAW(this.reference).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SourceOrSink) {
			final SourceOrSink temp = (SourceOrSink) obj;
			if (EqualsHelper.equals(this.reference, temp.getReference(), EQUALS_OPTIONS)) {
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
		if (Data.getInstance().getMapR().get(this) == null) {
			return null;
		}
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

	public SourceOrSink getDeepest() {
		if (this.combine == null || this.combine.isEmpty()) {
			return this;
		} else {
			int idMax = this.id;
			for (final String otherIdStr : this.combine.replace(" ", "").split(",")) {
				final int otherId = Integer.valueOf(otherIdStr).intValue();
				if (otherId > idMax) {
					idMax = otherId;
				}
			}
			if (idMax == this.id) {
				return this;
			} else {
				return Data.getInstance().getSourceOrSinkById(idMax);
			}
		}
	}

	public List<SourceOrSink> getAll() {
		final List<SourceOrSink> all = new ArrayList<>();
		all.add(this);
		if (this.combine != null && !this.combine.isEmpty()) {
			for (final String idStr : this.combine.replace(" ", "").split(",")) {
				final int id = Integer.valueOf(idStr).intValue();
				all.add(Data.getInstance().getSourceOrSinkById(id));
			}
		}
		return all;
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

	@Override
	public String toString() {
		return Helper.getLineNumberSafe(this.reference) + ":" + this.reference.getStatement().getStatementfull();
	}
}