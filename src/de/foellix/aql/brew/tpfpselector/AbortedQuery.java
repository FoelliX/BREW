package de.foellix.aql.brew.tpfpselector;

public class AbortedQuery {
	private long started;
	private long ended;

	protected AbortedQuery(long started, long ended) {
		this.started = started;
		this.ended = ended;
	}

	protected long getStarted() {
		return this.started;
	}

	protected long getEnded() {
		return this.ended;
	}

	protected void setStarted(long started) {
		this.started = started;
	}

	protected void setEnded(long ended) {
		this.ended = ended;
	}
}
