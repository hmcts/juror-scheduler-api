package uk.gov.hmcts.juror.scheduler.api.model.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.Map;

@Getter
@AllArgsConstructor
@Builder
public class TaskDetail {


    @Pattern(regexp = APIConstants.JOB_KEY_REGEX)
    @NotNull
    @JsonProperty("job_key")
    @Schema(description = "The Job this task relates too")
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
    @Schema(description = "The execution status of the Task")
    private Status status;

    @Length(min = 1, max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    @Schema(description = "A message received from the Job")
    private String message;

    @JsonProperty("post_actions_message")
    @Schema(description = "Messages received from post actions (always null if Job has no post actions)")
    private String postActionsMessage;

    @JsonProperty("meta_data")
    @Schema(description = "A map of data relating to the task")
    private Map<String, Object> metaData;
}
