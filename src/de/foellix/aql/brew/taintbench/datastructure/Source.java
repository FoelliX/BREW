
package de.foellix.aql.brew.taintbench.datastructure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "statement", "methodName", "className", "lineNo", "decompiledSourceLineNo", "targetName",
		"targetNo", "IRs" })
public class Source {

	@JsonProperty("statement")
	private String statement;
	@JsonProperty("methodName")
	private String methodName;
	@JsonProperty("className")
	private String className;
	@JsonProperty("lineNo")
	private int lineNo;
	@JsonProperty("decompiledSourceLineNo")
	private int decompiledSourceLineNo;
	@JsonProperty("targetName")
	private String targetName;
	@JsonProperty("targetNo")
	private int targetNo;
	@JsonProperty("IRs")
	private List<IR> iRs = null;

	@JsonProperty("statement")
	public String getStatement() {
		return this.statement;
	}

	@JsonProperty("statement")
	public void setStatement(String statement) {
		this.statement = statement;
	}

	@JsonProperty("methodName")
	public String getMethodName() {
		return this.methodName;
	}

	@JsonProperty("methodName")
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@JsonProperty("className")
	public String getClassName() {
		return this.className;
	}

	@JsonProperty("className")
	public void setClassName(String className) {
		this.className = className;
	}

	@JsonProperty("lineNo")
	public int getLineNo() {
		return this.lineNo;
	}

	@JsonProperty("lineNo")
	public void setLineNo(int lineNo) {
		this.lineNo = lineNo;
	}

	@JsonProperty("decompiledSourceLineNo")
	public int getdecompiledSourceLineNo() {
		return this.decompiledSourceLineNo;
	}

	@JsonProperty("decompiledSourceLineNo")
	public void setDecompiledSourceLineNo(int decompiledSourceLineNo) {
		this.decompiledSourceLineNo = decompiledSourceLineNo;
	}

	@JsonProperty("targetName")
	public String getTargetName() {
		return this.targetName;
	}

	@JsonProperty("targetName")
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	@JsonProperty("targetNo")
	public int getTargetNo() {
		return this.targetNo;
	}

	@JsonProperty("targetNo")
	public void setTargetNo(int targetNo) {
		this.targetNo = targetNo;
	}

	@JsonProperty("IRs")
	public List<IR> getIRs() {
		return this.iRs;
	}

	@JsonProperty("IRs")
	public void setIRs(List<IR> iRs) {
		this.iRs = iRs;
	}

}
