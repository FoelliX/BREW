package de.foellix.aql.brew.tpfpselector;

import de.foellix.aql.datastructure.Answer;

public class PreviousQuery {
	private int status;
	private Answer answer;
	private long started;
	private long ended;

	protected PreviousQuery(long started) {
		this.started = started;
	}

	protected int getStatus() {
		return this.status;
	}

	protected Answer getAnswer() {
		return this.answer;
	}

	protected long getStarted() {
		return this.started;
	}

	protected long getEnded() {
		return this.ended;
	}

	protected void setStatus(int status) {
		this.status = status;
	}

	protected void setAnswer(Answer answer) {
		this.answer = answer;
	}

	protected void setStarted(long started) {
		this.started = started;
	}

	protected void setEnded(long ended) {
		this.ended = ended;
	}
}
