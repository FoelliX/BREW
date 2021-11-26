
package de.foellix.aql.brew.taintbench.datastructure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "source", "sink", "intermediateFlows", "attributes", "creationDate", "ID", "description",
		"isNegative" })
public class Finding {

	@JsonProperty("source")
	private Source source;
	@JsonProperty("sink")
	private Sink sink;
	@JsonProperty("intermediateFlows")
	private List<IntermediateFlow> intermediateFlows = null;
	@JsonProperty("attributes")
	private Attributes attributes;
	@JsonProperty("creationDate")
	private String creationDate;
	@JsonProperty("ID")
	private int iD;
	@JsonProperty("description")
	private String description;
	@JsonProperty("isNegative")
	private boolean isNegative;

	@JsonProperty("source")
	public Source getSource() {
		return this.source;
	}

	@JsonProperty("source")
	public void setSource(Source source) {
		this.source = source;
	}

	@JsonProperty("sink")
	public Sink getSink() {
		return this.sink;
	}

	@JsonProperty("sink")
	public void setSink(Sink sink) {
		this.sink = sink;
	}

	@JsonProperty("intermediateFlows")
	public List<IntermediateFlow> getIntermediateFlows() {
		return this.intermediateFlows;
	}

	@JsonProperty("intermediateFlows")
	public void setIntermediateFlows(List<IntermediateFlow> intermediateFlows) {
		this.intermediateFlows = intermediateFlows;
	}

	@JsonProperty("attributes")
	public Attributes getAttributes() {
		return this.attributes;
	}

	@JsonProperty("attributes")
	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	@JsonProperty("creationDate")
	public String getCreationDate() {
		return this.creationDate;
	}

	@JsonProperty("creationDate")
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	@JsonProperty("ID")
	public int getID() {
		return this.iD;
	}

	@JsonProperty("ID")
	public void setID(int iD) {
		this.iD = iD;
	}

	@JsonProperty("description")
	public String getDescription() {
		return this.description;
	}

	@JsonProperty("description")
	public void setDescription(String description) {
		this.description = description;
	}

	@JsonProperty("isNegative")
	public boolean getIsNegative() {
		return this.isNegative;
	}

	@JsonProperty("isNegative")
	public void setIsNegative(boolean isNegative) {
		this.isNegative = isNegative;
	}

	@JsonProperty("isUnexpected")
	public boolean getIsUnexpected() {
		return this.isNegative;
	}

	@JsonProperty("isUnexpected")
	public void setIsUnexpected(boolean isUnexpected) {
		this.isNegative = isUnexpected;
	}
}