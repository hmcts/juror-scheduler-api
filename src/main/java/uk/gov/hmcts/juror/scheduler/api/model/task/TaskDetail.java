package uk.gov.hmcts.juror.scheduler.api.model.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class TaskDetail {


    @Pattern(regexp = APIConstants.JOB_KEY_REGEX)
    @NotNull
    @JsonProperty("job_key")
    private String jobKey;

    @Min(APIConstants.TASK_ID_MIN)
    @Max(APIConstants.TASK_ID_MAX)
    @NotNull
    @JsonProperty("task_id")
    private int taskId;

    @NotNull
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("last_updated_at")
    private LocalDateTime lastUpdatedAt;
    @NotNull
    private Status status;

    @Length(min = 1,max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String message;
}
