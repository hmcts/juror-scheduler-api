package uk.gov.hmcts.juror.scheduler.api.model.job.details;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.validation.CronExpression;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobType;

@Getter
@Setter
@NoArgsConstructor
@Schema(oneOf = {
    APIJobDetails.class
})
@SuperBuilder
public class JobDetails {

    @NotNull
    @Schema(description = "This is the type of Job the details is representing")
    protected JobType type;

    @NotNull
    @Valid
    @Schema(description = "This section stores information about the job")
    private Information information;

    @NotNull
    @Pattern(regexp = APIConstants.JOB_KEY_REGEX)
    @Schema(description = "The key is the unique identifier that should be assigned to this Job.")
    private String key;

    @JsonProperty("cron_expression")
    @CronExpression
    @Schema(description = "The cron expression that should be use to schedule this Job. "
        + "If none is provided the Job will be unscheduled as such will only run if manually triggered.")
    private String cronExpression;
}
