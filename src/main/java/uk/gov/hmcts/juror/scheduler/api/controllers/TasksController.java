package uk.gov.hmcts.juror.scheduler.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.config.PermissionConstants;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.TaskSearchFilter;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.api.model.error.InternalServerError;
import uk.gov.hmcts.juror.standard.api.model.error.InvalidPayloadError;
import uk.gov.hmcts.juror.standard.api.model.error.NotFoundError;
import uk.gov.hmcts.juror.standard.api.model.error.UnauthorisedError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@Tag(name = "Tasks")
@Slf4j
@RequestMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class TasksController {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Autowired
    public TasksController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Task.SEARCH + "')")
    @Operation(summary = "Searches for a list of tasks", description = "Returns the details of the found tasks",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of Tasks.",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(
                        schema = @Schema(implementation = TaskDetail.class)))}),
            @ApiResponse(responseCode = "400", description = "Invalid Parameters",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = InvalidPayloadError.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorised",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = UnauthorisedError.class))}),
            @ApiResponse(responseCode = "404", description = "Not Found",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = NotFoundError.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = InternalServerError.class))})})
    public ResponseEntity<List<TaskDetail>> getTasks(
        @RequestParam(name = "from_date", required = false) @Schema(name = "from_date", description =
            "The date at which we should "
                + "return any failures from.", defaultValue = "Today's date minus 7 days")
        @Valid LocalDateTime fromDate,
        @RequestParam(name = "job_key", required = false) @Schema(name = "job_key", description = "The job to search "
            + "for. (If none provided all Jobs will be searched)")
        @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @Valid String jobKey,
        @RequestParam(name = "status", required = false) @Schema(name = "status", description = "The statuses to "
            + "filter by") @Valid Set<@NotNull Status> statuses) {

        return ResponseEntity.ok(taskMapper.toTaskList(taskService.getTasks(
            TaskSearchFilter.builder().jobKey(jobKey).fromDate(fromDate).statuses(statuses).build())));
    }
}
