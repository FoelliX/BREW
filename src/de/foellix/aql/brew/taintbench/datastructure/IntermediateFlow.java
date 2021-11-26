
package de.foellix.aql.brew.taintbench.datastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "statement", "methodName", "className", "lineNo", "decompiledSourceLineNo", "ID" })
public class IntermediateFlow {

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
	@JsonProperty("ID")
	private int iD;

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

	@JsonProperty("ID")
	public int getID() {
		return this.iD;
	}

	@JsonProperty("ID")
	public void setID(int iD) {
		this.iD = iD;
	}

}
