package uk.gov.hmcts.juror.scheduler.api.model.job.details.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("STATUS_CODE")
public class StatusCodeAPIValidation extends APIValidation {
    @Schema(type = "string", allowableValues = "STATUS_CODE")
    @NotNull
    @Override
    public ValidationType getType() {
        return ValidationType.STATUS_CODE;
    }

    @NotNull
    @Min(100)
    @Max(599)
    @JsonProperty("expected_status_code")
    @Schema(description = "The expected status code from the API request. Job will fail if something other then this is returned.")
    private Integer expectedStatusCode;
}
