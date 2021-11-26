
package de.foellix.aql.brew.taintbench.datastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "IRstatement"
})
public class IR {

    @JsonProperty("type")
    private String type;
    @JsonProperty("IRstatement")
    private String iRstatement;

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("IRstatement")
    public String getIRstatement() {
        return iRstatement;
    }

    @JsonProperty("IRstatement")
    public void setIRstatement(String iRstatement) {
        this.iRstatement = iRstatement;
    }

}
