package uk.gov.hmcts.juror.scheduler.api.model.job.details.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.JobDetails;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobType;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class APIJobDetails extends JobDetails {

    @NotNull
    @Schema(description = "The method to use when making the API request")
    private APIMethod method;

    @NotNull
    @Schema(description = "The fully quantified url to call when making the API request")
    @URL
    private String url;

    @Schema(description = "All the headers that should be included when making the API Request")
    @Size(min = 1, max = 100)
    private Map<
        @NotNull @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG) String,
        @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG) String> headers;

    @Schema(description = "If present an authentication token will automatically be added to your API request based "
        + "on the selected system.")
    @JsonProperty("authentication_default")
    private AuthenticationDefaults authenticationDefault;

    @Schema(description = "The payload to include along with the request (Note this should not be present for GET "
        + "requests)")
    @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String payload;

    @Schema(description = "A list of validations that should be applied to the response after the API request has "
        + "been made. If any of these fail the task will fail.")
    @NotEmpty
    @Size(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_SHORT)
    private List<@Valid ? extends APIValidation> validations;

    @Schema(allowableValues = "API")
    @Override
    public JobType getType() {
        return type;
    }
}
