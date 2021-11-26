
package de.foellix.aql.brew.taintbench.datastructure;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fileName",
    "day",
    "findings"
})
public class TaintBenchCase {

    @JsonProperty("fileName")
    private String fileName;
    @JsonProperty("day")
    private String day;
    @JsonProperty("findings")
    private List<Finding> findings = null;

    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    @JsonProperty("fileName")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @JsonProperty("day")
    public String getDay() {
        return day;
    }

    @JsonProperty("day")
    public void setDay(String day) {
        this.day = day;
    }

    @JsonProperty("findings")
    public List<Finding> getFindings() {
        return findings;
    }

    @JsonProperty("findings")
    public void setFindings(List<Finding> findings) {
        this.findings = findings;
    }

}
