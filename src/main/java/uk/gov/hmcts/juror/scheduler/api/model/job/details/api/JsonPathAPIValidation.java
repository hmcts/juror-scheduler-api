package uk.gov.hmcts.juror.scheduler.api.model.job.details.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.juror.scheduler.api.validation.JsonPath;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("JSON_PATH")
public class JsonPathAPIValidation extends APIValidation {

    @NotNull
    @JsonPath
    @Schema(description = "This is the JSON path that the response payload will be evaluated against.")
    private String path;

    @NotNull
    @Schema(description = "This is the expected value that the JsonPath should return when evaluated against the API response")
    @JsonProperty("expected_response")
    private String expectedResponse;


    @Schema(type = "string", allowableValues = "JSON_PATH")
    @NotNull
    @Override
    public ValidationType getType() {
        return ValidationType.JSON_PATH;
    }
}
