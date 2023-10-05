package uk.gov.hmcts.juror.scheduler.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.JobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.config.PermissionConstants;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.api.model.error.InternalServerError;
import uk.gov.hmcts.juror.standard.api.model.error.InvalidPayloadError;
import uk.gov.hmcts.juror.standard.api.model.error.NotFoundError;
import uk.gov.hmcts.juror.standard.api.model.error.UnauthorisedError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.IncorrectPayloadForJobTypeError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.NotAScheduledJobError;

import java.util.List;

@RestController
@Tag(name = "Job")
@Slf4j
@RequestMapping(value = "/job/{job-key}", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.AvoidDuplicateLiterals"
})
public class JobController {

    private final JobService jobService;
    private final TaskService taskService;
    private final JobDetailsMapper jobDetailsMapper;
    private final TaskMapper taskMapper;

    @Autowired
    public JobController(JobService jobService,
                         TaskService taskService,
                         JobDetailsMapper jobDetailsMapper,
                         TaskMapper taskMapper) {
        this.jobService = jobService;
        this.taskService = taskService;
        this.jobDetailsMapper = jobDetailsMapper;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.VIEW + "')")
    @Operation(summary = "Get Job Details", description = "Returns the details of the selected Job",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Found", content = {@Content(mediaType =
                                                                                                             MediaType.APPLICATION_JSON_VALUE,
                                                                                                     schema = @Schema(
                                                                                                             implementation = JobDetails.class))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<APIJobDetailsResponse> getJobDetails(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key") @Valid String jobKey) {
        return ResponseEntity.ok(jobDetailsMapper.toAPIJobDetailsResponse(jobService.getJob(jobKey)));
    }

    @PutMapping("/enable")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.ENABLE + "')")
    @Operation(summary = "Enable Job", description = "Enables the specified Job, allowing it to be automatically " +
            "executed at time intervals specified in the Jobs Cron Expression. (This is only valid for Scheduled Jobs"
            + ". I"
            +
            ".e ones with a Cron expression)",
               responses = {
                       @ApiResponse(responseCode = "202", description = "Job Enabled"),
                       @ApiResponse(responseCode = "422", description = "Business Validation Rule", content =
                               {@Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(anyOf =
                                       {NotAScheduledJobError.class, JobAlreadyEnabledError.class}))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<Void> enableJob(@Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key"
    ) @Valid String jobKey) {
        jobService.enable(jobKey);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PutMapping("/disable")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.DISABLE + "')")
    @Operation(summary = "Disable Job", description = "Disables the specified Job, allowing it to be automatically " +
            "executed at time intervals specified in the Jobs Cron Expression. (This is only valid for Scheduled Jobs"
            + ". I"
            +
            ".e ones with a Cron expression)",
               responses = {
                       @ApiResponse(responseCode = "202", description = "Job Disabled"),
                       @ApiResponse(responseCode = "422", description = "Business Validation Rule", content =
                               {@Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(anyOf =
                                       {NotAScheduledJobError.class, JobAlreadyDisabledError.class}))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<Void> disableJob(@Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job" +
            "-key") @Valid String jobKey) {
        jobService.disable(jobKey);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping()
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.DELETE + "')")
    @Operation(summary = "Delete Job", description = "This will delete the Job from the System as well as any " +
            "associated Tasks",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Job Deleted"),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<Void> deleteJob(@Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key"
    ) @Valid String jobKey) {
        jobService.deleteJob(jobKey);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/run")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.RUN + "')")
    @Operation(summary = "Run Job", description = "Trigger the Job to run immediately (Bypassing any schedules if " +
            "set, as well as enabled/disabled state).",
               responses = {
                       @ApiResponse(responseCode = "202", description = "Job queued to run."),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<Void> runJob(@Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name =
            "job-key") @Valid String jobKey) {
        jobService.executeJob(jobKey);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.API_UPDATE + "')")
    @Operation(summary = "Updates Job Data", description = "Will update the specified Job with the details you " +
            "provide (any details you do not provide will remain unchanged)",
               responses = {
                       @ApiResponse(responseCode = "200", description = "Job updated successfully.", content =
                               {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation =
                                       JobDetails.class))}),
                       @ApiResponse(responseCode = "400", description = "Invalid Payload",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InvalidPayloadError.class))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "422", description = "Business Validation Rule", content =
                               {@Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                         schema = @Schema(implementation =
                                                 IncorrectPayloadForJobTypeError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<APIJobDetailsResponse> updateJob(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name =
                    "job-key") @Valid String jobKey, @RequestBody @Valid APIJobPatch jobPatch) {
        return ResponseEntity.ok(jobDetailsMapper.toAPIJobDetailsResponse(jobService.updateJob(jobKey, jobPatch)));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.VIEW + "')")
    @Operation(summary = "Get all the tasks for a Job",
               description = "Returns the tasks for a Job",
               responses = {
                       @ApiResponse(responseCode = "200", description = "TaskEntity details found for this Job",
                                    content =
                                            {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                      array = @ArraySchema(schema =
                                                      @Schema(implementation = TaskDetail.class)))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<List<TaskDetail>> getJobTasks(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key") @Valid String jobKey
    ) {
        return new ResponseEntity<>(taskMapper.toTaskList(taskService.getTasks(jobKey)), HttpStatus.OK);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.VIEW + "') or hasAuthority('" + PermissionConstants.Job.VIEW_STATUS + "')")
    @Operation(summary = "Get the latest task for a Job",
               description = "Returns the latest task for a Job",
               responses = {
                       @ApiResponse(responseCode = "200", description = "TaskEntity details found for this Job",
                                    content =
                                            {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                      schema = @Schema(implementation = TaskDetail.class))}),
                       @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                                                                                                                    MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                            schema =
                                                                                                            @Schema(
                                                                                                                    implementation = UnauthorisedError.class))}),
                       @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                                                                                                                 MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                                                                                         schema =
                                                                                                         @Schema(
                                                                                                                 implementation = NotFoundError.class))}),
                       @ApiResponse(responseCode = "500", description = "Internal Server Error",
                                    content = {@Content(mediaType =
                                                                MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                                                        @Schema(implementation = InternalServerError.class))})
               })
    public ResponseEntity<TaskDetail> getJobStatus(
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @PathVariable(name = "job-key") @Valid String jobKey
    ) {
        return new ResponseEntity<>(taskMapper.toTask(taskService.getLatestTask(jobKey)), HttpStatus.OK);
    }
}
