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
@JsonTypeName("MAX_RESPONSE_TIME")
public class MaxResponseTimeAPIValidation extends APIValidation {

    @NotNull
    @Min(1)
    @Max(30_000)
    @JsonProperty("max_response_time_ms")
    @Schema(description = "This is the maximum response time that is allowed before the TaskEntity will fail.")
    private Integer maxResponseTimeMS;

    @Schema(type = "string", allowableValues = "MAX_RESPONSE_TIME")
    @NotNull
    @Override
    public ValidationType getType() {
        return ValidationType.MAX_RESPONSE_TIME;
    }
}
