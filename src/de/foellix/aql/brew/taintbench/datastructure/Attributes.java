
package de.foellix.aql.brew.taintbench.datastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "appendToString", "array", "callbacks", "collections", "implicitFlows", "interAppCommunication",
		"interComponentCommunication", "lifecycle", "nonStaticField", "partialFlow", "pathConstraints", "payload",
		"reflection", "staticField", "threading" })
public class Attributes {

	@JsonProperty("appendToString")
	private boolean appendToString;
	@JsonProperty("array")
	private boolean array;
	@JsonProperty("callbacks")
	private boolean callbacks;
	@JsonProperty("collections")
	private boolean collections;
	@JsonProperty("implicitFlows")
	private boolean implicitFlows;
	@JsonProperty("interAppCommunication")
	private boolean interAppCommunication;
	@JsonProperty("interComponentCommunication")
	private boolean interComponentCommunication;
	@JsonProperty("lifecycle")
	private boolean lifecycle;
	@JsonProperty("nonStaticField")
	private boolean nonStaticField;
	@JsonProperty("partialFlow")
	private boolean partialFlow;
	@JsonProperty("pathConstraints")
	private boolean pathConstraints;
	@JsonProperty("payload")
	private boolean payload;
	@JsonProperty("reflection")
	private boolean reflection;
	@JsonProperty("staticField")
	private boolean staticField;
	@JsonProperty("threading")
	private boolean threading;

	@JsonProperty("appendToString")
	public boolean isAppendToString() {
		return this.appendToString;
	}

	@JsonProperty("array")
	public boolean isArray() {
		return this.array;
	}

	@JsonProperty("callbacks")
	public boolean isCallbacks() {
		return this.callbacks;
	}

	@JsonProperty("collections")
	public boolean isCollections() {
		return this.collections;
	}

	@JsonProperty("implicitFlows")
	public boolean isimplicitFlows() {
		return this.implicitFlows;
	}

	@JsonProperty("interAppCommunication")
	public boolean isInterAppCommunication() {
		return this.interAppCommunication;
	}

	@JsonProperty("interComponentCommunication")
	public boolean isInterComponentCommunication() {
		return this.interComponentCommunication;
	}

	@JsonProperty("lifecycle")
	public boolean isLifecycle() {
		return this.lifecycle;
	}

	@JsonProperty("nonStaticField")
	public boolean isNonStaticField() {
		return this.nonStaticField;
	}

	@JsonProperty("partialFlow")
	public boolean isPartialFlow() {
		return this.partialFlow;
	}

	@JsonProperty("pathConstraints")
	public boolean isPathConstraints() {
		return this.pathConstraints;
	}

	@JsonProperty("payload")
	public boolean isPayload() {
		return this.payload;
	}

	@JsonProperty("reflection")
	public boolean isReflection() {
		return this.reflection;
	}

	@JsonProperty("staticField")
	public boolean isStaticField() {
		return this.staticField;
	}

	@JsonProperty("threading")
	public boolean isThreading() {
		return this.threading;
	}

	@JsonProperty("appendToString")
	public void setAppendToString(boolean appendToString) {
		this.appendToString = appendToString;
	}

	@JsonProperty("array")
	public void setArray(boolean array) {
		this.array = array;
	}

	@JsonProperty("callbacks")
	public void setCallbacks(boolean callbacks) {
		this.callbacks = callbacks;
	}

	@JsonProperty("collections")
	public void setCollections(boolean collections) {
		this.collections = collections;
	}

	@JsonProperty("implicitFlows")
	public void setimplicitFlows(boolean implicitFlows) {
		this.implicitFlows = implicitFlows;
	}

	@JsonProperty("interAppCommunication")
	public void setInterAppCommunication(boolean interAppCommunication) {
		this.interAppCommunication = interAppCommunication;
	}

	@JsonProperty("interComponentCommunication")
	public void setInterComponentCommunication(boolean interComponentCommunication) {
		this.interComponentCommunication = interComponentCommunication;
	}

	@JsonProperty("lifecycle")
	public void setLifecycle(boolean lifecycle) {
		this.lifecycle = lifecycle;
	}

	@JsonProperty("nonStaticField")
	public void setNonStaticField(boolean nonStaticField) {
		this.nonStaticField = nonStaticField;
	}

	@JsonProperty("partialFlow")
	public void setPartialFlow(boolean partialFlow) {
		this.partialFlow = partialFlow;
	}

	@JsonProperty("pathConstraints")
	public void setPathConstraints(boolean pathConstraints) {
		this.pathConstraints = pathConstraints;
	}

	@JsonProperty("payload")
	public void setPayload(boolean payload) {
		this.payload = payload;
	}

	@JsonProperty("reflection")
	public void setReflection(boolean reflection) {
		this.reflection = reflection;
	}

	@JsonProperty("staticField")
	public void setStaticField(boolean staticField) {
		this.staticField = staticField;
	}

	@JsonProperty("threading")
	public void setThreading(boolean threading) {
		this.threading = threading;
	}

}