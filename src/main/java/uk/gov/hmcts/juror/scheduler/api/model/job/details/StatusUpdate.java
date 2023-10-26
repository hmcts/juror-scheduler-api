package uk.gov.hmcts.juror.scheduler.api.model.job.details;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class StatusUpdate {

    @NotNull
    @Schema(description = "The new status of the task")
    private Status status;
    @Schema(description = "A brief message that can be displayed back to the user to help explain the status change")
    @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String message;

    @JsonProperty("meta_data")
    @Schema(description = "A map of data relating to the task")
    private Map<String, String> metaData;
}
