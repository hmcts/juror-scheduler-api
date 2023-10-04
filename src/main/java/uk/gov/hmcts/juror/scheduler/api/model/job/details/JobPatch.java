package uk.gov.hmcts.juror.scheduler.api.model.job.details;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.juror.scheduler.api.validation.CronExpression;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class JobPatch {

    @JsonProperty("cron_expression")
    @Schema(description = "This is the cron expression that will be used to setup automated triggering of this Job")
    @CronExpression
    private String cronExpression;

    @Schema(description = "The information about the Job")
    @Valid
    private Information information;
}
