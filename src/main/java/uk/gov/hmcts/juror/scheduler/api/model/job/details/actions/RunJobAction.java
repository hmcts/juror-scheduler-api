package uk.gov.hmcts.juror.scheduler.api.model.job.details.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeName("RUN_JOB")
@Accessors(chain = true)
public class RunJobAction extends Action {
    @JsonProperty("job_key")
    @NotBlank
    private String jobKey;

    @Schema(type = "string", allowableValues = "RUN_JOB")
    @NotNull
    @Override
    public ActionType getType() {
        return ActionType.RUN_JOB;
    }

}
