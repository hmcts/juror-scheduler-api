package uk.gov.hmcts.juror.scheduler.service.jobs;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobResult;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@DisallowConcurrentExecution
public class APIJob implements Job {
    private final JobService jobService;
    private final TaskService taskService;
    private final EntityManager entityManager;

    @Autowired
    public APIJob(JobService jobService, TaskService taskService, EntityManager entityManager) {
        this.jobService = jobService;
        this.taskService = taskService;
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings({
        "PMD.LawOfDemeter",
        "PMD.AvoidInstanceofChecksInCatchClause"
    })
    public void execute(JobExecutionContext context) {
        String jobKey = null;
        TaskEntity task = null;

        try {
            jobKey = getJobKeyFromContext(context);
            final APIJobDetailsEntity apiJobDetailsEntity = jobService.getJob(jobKey);
            task = taskService.createTask(apiJobDetailsEntity);


            final RequestSpecification requestSpecification = setupRequest(apiJobDetailsEntity, task);
            final Response response = triggerRequest(requestSpecification, apiJobDetailsEntity);

            LocalDateTime lastUpdated = task.getLastUpdatedAt();
            entityManager.refresh(task);//Refresh the task from the database

            boolean hasTaskBeenUpdated = lastUpdated != null && lastUpdated.isEqual(task.getLastUpdatedAt());

            //This method will automatically update the task with  the updated status / message
            validateResponse(response, apiJobDetailsEntity, task);


            //If the task has not been updated OR task has been updated but validation has failed, then save the task
            if (!hasTaskBeenUpdated || task.getStatus() != Status.VALIDATION_PASSED) {
                task = taskService.saveTask(task);
            } else {
                log.info("Task was updated while processing, not updating the task");
            }
            log.info("Complete task for Job: " + jobKey);
        } catch (Exception exception) {
            log.error("Failed to run Job" + (jobKey == null ? "" : " with Job key: " + jobKey), exception);
            if (task != null) {
                task.setStatus(Status.FAILED_UNEXPECTED_EXCEPTION);
                taskService.saveTask(task);
            }
            context.setResult(
                JobResult.builder()
                    .passed(false)
                    .error(JobResult.ErrorDetails.builder()
                        .message(exception.getMessage())
                        .throwable(exception)
                        .build())
                    .build()
            );
        }
    }

    private String getJobKeyFromContext(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        if (!jobDataMap.containsKey("key")) {
            throw new InternalServerException("Job Key not found");
        }
        return jobDataMap.getString("key");
    }


    private RequestSpecification setupRequest(APIJobDetailsEntity apiJobDetailsEntity, TaskEntity task) {
        RequestSpecification requestSpecification = RestAssured.given().log().all();
        if (apiJobDetailsEntity.getPayload() != null) {
            requestSpecification.body(apiJobDetailsEntity.getPayload());
        }

        if (!CollectionUtils.isEmpty(apiJobDetailsEntity.getHeaders())) {
            apiJobDetailsEntity.getHeaders().forEach(requestSpecification::header);
        }
        requestSpecification.header("job_key", apiJobDetailsEntity.getKey());
        requestSpecification.header("task_id", task.getTaskId());

        if (apiJobDetailsEntity.getAuthenticationDefault() != null) {
            apiJobDetailsEntity.getAuthenticationDefault().addAuthentication(apiJobDetailsEntity,
                requestSpecification);
        }
        return requestSpecification;
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private void validateResponse(Response response, APIJobDetailsEntity apiJobDetailsEntity, TaskEntity task) {
        StringBuilder messageBuilder = new StringBuilder();
        AtomicReference<Boolean> passed = new AtomicReference<>(true);
        apiJobDetailsEntity.getValidations().forEach(validation -> {
            APIValidationEntity.Result result = validation.validate(response, apiJobDetailsEntity);
            log.trace("Validating: " + validation.getType() + " Result: " + result.isPassed() + " - "
                + result.getMessage());
            if (result.isPassed()) {
                //No need to get message if passed
                return;
            }
            passed.set(false);
            messageBuilder.append(validation.getType().name()).append(": ").append(result.getMessage()).append('\n');
        });
        task.setStatus(Boolean.TRUE.equals(passed.get()) ? Status.VALIDATION_PASSED : Status.VALIDATION_FAILED);
        if (Boolean.FALSE.equals(passed.get())) {
            task.setMessage(messageBuilder.toString());
        }
    }

    private Response triggerRequest(RequestSpecification requestSpecification,
                                    APIJobDetailsEntity apiJobDetailsEntity) {
        return requestSpecification.request(
            Method.valueOf(apiJobDetailsEntity.getMethod().name()),
            apiJobDetailsEntity.getUrl());
    }
}
