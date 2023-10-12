package uk.gov.hmcts.juror.scheduler.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.config.PermissionConstants;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.standard.api.model.error.InternalServerError;
import uk.gov.hmcts.juror.standard.api.model.error.InvalidPayloadError;
import uk.gov.hmcts.juror.standard.api.model.error.NotFoundError;
import uk.gov.hmcts.juror.standard.api.model.error.UnauthorisedError;

import java.util.List;
import java.util.Set;

@RestController
@Tag(name = "Jobs")
@Slf4j
@RequestMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@SuppressWarnings({
        "PMD.ExcessiveImports"
})
public class JobsController {

    private final JobService jobService;
    private final JobDetailsMapper jobDetailsMapper;

    @Autowired
    public JobsController(JobService jobService, JobDetailsMapper jobDetailsMapper) {
        this.jobService = jobService;
        this.jobDetailsMapper = jobDetailsMapper;
    }

    @PostMapping("/api")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.API_CREATE + "')")
    @Operation(summary = "Create a new API Job",
            description = "An API Job will send an API call to a third party and validate its response when triggered. " +
                    "This API allows you to create a new API Job.",
//            security = {@SecurityRequirement(name = "permission", scopes = PermissionConstants.Job.API_CREATE)},
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "400", description = "Invalid Payload", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                    @Schema(implementation = InvalidPayloadError.class))}),
                    @ApiResponse(responseCode = "409", description = "Key already in use", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation =
                            KeyAlreadyInUseError.class))}),
                    @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = UnauthorisedError.class))}),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                    @Schema(implementation = InternalServerError.class))}),
            }
    )
    public ResponseEntity<Void> createAPIJob(@Valid @NotNull @RequestBody APIJobDetails apiJob) {
        this.jobService.createJob(apiJob);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Job.SEARCH + "')")
    @Operation(summary = "Searches for a list of tasks",
            description = "Returns the details of the found tasks",
//            security = {@SecurityRequirement(name = "permission", scopes = Permission.Constants.JOBS_SEARCH)},
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of Tasks.", content = {@Content(mediaType =
                            MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TaskDetail.class)))}),
                    @ApiResponse(responseCode = "400", description = "Invalid Parameters", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation =
                            InvalidPayloadError.class))}),
                    @ApiResponse(responseCode = "401", description = "Unauthorised", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = UnauthorisedError.class))}),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = NotFoundError.class))}),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(mediaType =
                            MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema =
                    @Schema(implementation = InternalServerError.class))})
            }
    )
    public ResponseEntity<List<APIJobDetailsResponse>> getJobs(
            @RequestParam(name = "job_key", required = false) @Schema(name = "job_key",
                    description = "The job to search for on. (If none provided all Jobs will be searched)")
            @Pattern(regexp = APIConstants.JOB_KEY_REGEX) @Valid String jobKey,
            @RequestParam(name = "tag", required = false) @Schema(name = "tag",
                    description = "The tags to filter by")
            @Valid Set<@Length(max = APIConstants.DEFAULT_MAX_LENGTH_SHORT) @NotBlank String> tags
    ) {
        return ResponseEntity.ok(jobDetailsMapper.toJobDetailsJobDetailsList(jobService.getJobs(
                JobSearchFilter.builder()
                        .jobKey(jobKey)
                        .tags(tags)
                        .build())));
    }
}
