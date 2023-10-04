package uk.gov.hmcts.juror.scheduler.service.jobs;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestLogSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        APIJob.class
    }
)
@DisplayName("APIJob")
class APIJobTest {

    @MockBean
    private JobService jobService;

    @MockBean
    private TaskService taskService;

    @Autowired
    private APIJob apiJob;

    private MockedStatic<RestAssured> restAssuredMockedStatic;

    @MockBean
    private RequestSpecification requestSpecification;
    @MockBean
    private Response response;

    @MockBean
    private JobExecutionContext context;

    private TaskEntity taskEntity;

    private final String jobKey = "ABC123";

    @BeforeEach
    public void beforeEach() {
        restAssuredMockedStatic = Mockito.mockStatic(RestAssured.class);
        RequestLogSpecification requestLogSpecification = mock(RequestLogSpecification.class);

        restAssuredMockedStatic.when(RestAssured::given).thenReturn(requestSpecification);

        when(requestSpecification.log()).thenReturn(requestLogSpecification);
        when(requestLogSpecification.all()).thenReturn(requestSpecification);

    }

    @AfterEach
    public void afterEach() {
        if (restAssuredMockedStatic != null) {
            restAssuredMockedStatic.close();
        }
    }

    private void runStandardSetup(APIJobDetailsEntity apiJobDetailsEntity) {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", apiJobDetailsEntity.getKey());

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobService.getJob(eq(apiJobDetailsEntity.getKey()))).thenReturn(apiJobDetailsEntity);

        when(requestSpecification.request(eq(Method.valueOf(apiJobDetailsEntity.getMethod().name())),
            eq(apiJobDetailsEntity.getUrl()))).thenReturn(response);

        taskEntity = new TaskEntity();
        taskEntity.setJob(apiJobDetailsEntity);
        taskEntity.setTaskId(1L);
        taskEntity.setStatus(Status.PENDING);
        when(taskService.createTask(eq(apiJobDetailsEntity))).thenReturn(taskEntity);
    }


    private void runStandardVerification(APIJobDetailsEntity apiJobDetailsEntity) {
        int defaultHeaderCount = 2;

        //Validate payload setup correctly
        if (apiJobDetailsEntity.getPayload() == null) {
            verify(requestSpecification, never()).body(anyString());
        } else {
            verify(requestSpecification, times(1)).body(eq(apiJobDetailsEntity.getPayload()));
        }

        //Validate Headers setup correctly
        if (apiJobDetailsEntity.getHeaders() == null || apiJobDetailsEntity.getHeaders().isEmpty()) {
            verify(requestSpecification, times(defaultHeaderCount)).header(anyString(), any());
        } else {
            verify(requestSpecification, times(apiJobDetailsEntity.getHeaders().size() + defaultHeaderCount)).header(anyString(), any());
            apiJobDetailsEntity.getHeaders().forEach((key, value) -> verify(requestSpecification, times(1)).header(eq(key), eq(value)));
        }
        verify(requestSpecification, times(1)).header(eq("job_key"), eq(apiJobDetailsEntity.getKey()));
        verify(requestSpecification, times(1)).header(eq("task_id"), eq(taskEntity.getTaskId()));

        //Validate authentication default called correctly
        if (apiJobDetailsEntity.getAuthenticationDefault() != null) {
            verify(apiJobDetailsEntity.getAuthenticationDefault(), times(1)).addAuthentication(eq(apiJobDetailsEntity),
                eq(requestSpecification));
        }

        //Validate validations executed correctly
        StringBuilder expectedMessageBuilder = new StringBuilder();
        AtomicBoolean passed = new AtomicBoolean(true);
        apiJobDetailsEntity.getValidations().forEach(validation -> {
            APIValidationEntity.Result result = validation.validate(response, apiJobDetailsEntity);
            if (!result.isPassed()) {
                passed.set(false);
                expectedMessageBuilder.append(validation.getType().name()).append(": ").append(result.getMessage()).append(
                    "\n");
            }
        });
        Assertions.assertEquals(passed.get() ? Status.VALIDATION_PASSED : Status.VALIDATION_FAILED, taskEntity.getStatus());
        assertEquals(passed.get() ? null : expectedMessageBuilder.toString(), taskEntity.getMessage());

        //Validate Request was sent correctly
        verify(requestSpecification, times(1))
            .request(eq(Method.valueOf(apiJobDetailsEntity.getMethod().name())), eq(apiJobDetailsEntity.getUrl()));
        verify(taskService, times(1)).saveTask(eq(taskEntity));
    }

    @Test
    void positive_typical() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(), ValidationType.STATUS_CODE));
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    void positive_with_payload() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(), ValidationType.MAX_RESPONSE_TIME));
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.POST)
            .payload("this is my payload")
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    void positive_with_headers() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(), ValidationType.MAX_RESPONSE_TIME));

        Map<String, String> headers = new HashMap<>();
        headers.put("My Header", "My Header value");
        headers.put("Content-Type", "text/plain");

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.POST)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .headers(headers)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    void positive_with_authentication_default() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(), ValidationType.STATUS_CODE));

        AuthenticationDefaults authenticationDefaults = mock(AuthenticationDefaults.class);

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .authenticationDefault(authenticationDefaults)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    void negative_key_not_found() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);

        InternalServerException exception = assertThrows(InternalServerException.class, () -> apiJob.execute(context));

        assertEquals("Job Key not found", exception.getMessage());
    }

    @Test
    void negative_job_not_found() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", jobKey);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobService.getJob(eq(jobKey))).thenThrow(new NotFoundException("Job not found for key: " + jobKey));

        NotFoundException exception = assertThrows(NotFoundException.class, () -> apiJob.execute(context));

        assertEquals("Job not found for key: " + jobKey, exception.getMessage());
    }

    @Test
    void negative_unexpected_exception_before_task_creation() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", jobKey);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);
        RuntimeException cause = new RuntimeException("An example unexpected error");
        when(jobService.getJob(eq(jobKey))).thenThrow(cause);

        InternalServerException exception = assertThrows(InternalServerException.class, () -> apiJob.execute(context));

        assertEquals("Unexpected exception when executing Job for Key " + jobKey, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void negative_unexpected_exception_after_task_creation() {
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .build();
        runStandardSetup(apiJobDetailsEntity);
        RuntimeException cause = new RuntimeException("An example unexpected error");
        when(requestSpecification.request(eq(Method.valueOf(apiJobDetailsEntity.getMethod().name())),
            eq(apiJobDetailsEntity.getUrl()))).thenThrow(cause);

        InternalServerException exception = assertThrows(InternalServerException.class, () -> apiJob.execute(context));

        assertEquals("Unexpected exception when executing Job for Key " + jobKey, exception.getMessage());
        assertEquals(cause, exception.getCause());
        Assertions.assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, taskEntity.getStatus());
    }

    @Test
    void negative_validation_failed_single() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("FAILED for XYZ").build(),
            ValidationType.STATUS_CODE));


        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    void negative_validation_failed_multiple() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(), ValidationType.STATUS_CODE));
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("Failed for abc").build(),
            ValidationType.MAX_RESPONSE_TIME));
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("Failed for xyz").build(),
            ValidationType.JSON_PATH));

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(jobKey)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }


    private static class TestAPIJobDetailsEntity extends APIValidationEntity {

        private final Result result;
        private final ValidationType type;

        public TestAPIJobDetailsEntity(Result result, ValidationType type) {
            this.result = result;
            this.type = type;
        }

        @Override
        public Result validate(Response response, APIJobDetailsEntity jobData) {
            return result;
        }

        @Override
        public ValidationType getType() {
            return type;
        }
    }
}
