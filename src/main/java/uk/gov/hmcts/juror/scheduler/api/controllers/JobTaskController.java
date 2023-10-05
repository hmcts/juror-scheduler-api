package uk.gov.hmcts.juror.scheduler.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.config.PermissionConstants;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.standard.api.model.error.InternalServerError;
import uk.gov.hmcts.juror.standard.api.model.error.NotFoundError;
import uk.gov.hmcts.juror.standard.api.model.error.UnauthorisedError;

@RestController
@Tag(name = "TaskEntity")
@Slf4j
@RequestMapping(value = "/job/{job-key}/task/{task-id}", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class JobTaskController {


    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Autowired
    public JobTaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.Task.VIEW + "')")
    @Operation(summary = "Returns task details",
            description = "Returns the details of the selected TaskEntity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Returns the task details", content =
                            {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TaskDetail.class))}),
                    @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = UnauthorisedError.class))}),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = NotFoundError.class))}),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                    @Schema(implementation = InternalServerError.class))})
            })
    public ResponseEntity<TaskDetail> getTaskDetail(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key") @Schema(type = "string") @Valid String jobKey,
            @Min(APIConstants.TASK_ID_MIN) @Max(APIConstants.TASK_ID_MAX) @PathVariable(name = "task-id") @Schema(type =
                    "integer") @Valid long taskId) {
        return ResponseEntity.ok(taskMapper.toTask(taskService.getLatestTask(jobKey, taskId)));
    }

    @PutMapping("/status")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Task.STATUS_UPDATE + "')")
    @Operation(summary = "Update TaskEntity Status",
            description = "This API will update the task status after execution. This is designed to allow third party " +
                    "systems to inform the scheduler that an asynchronous task has successfully or unsuccessfully executed.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Successfully updated TaskEntity status", content =
                            {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TaskDetail.class))}),
                    @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = UnauthorisedError.class))}),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = NotFoundError.class))}),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                    @Schema(implementation = InternalServerError.class))})
            })
    public ResponseEntity<Void> updateTaskStatus(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key") @Schema(type = "string") @Valid String jobKey,
            @Min(APIConstants.TASK_ID_MIN) @Max(APIConstants.TASK_ID_MAX) @PathVariable(name = "task-id") @Schema(type =
                    "integer") @Valid long taskId,
            @Valid @RequestBody StatusUpdate statusUpdate) {
        taskService.updateStatus(jobKey, taskId, statusUpdate);
        return ResponseEntity.accepted().build();
    }
}
