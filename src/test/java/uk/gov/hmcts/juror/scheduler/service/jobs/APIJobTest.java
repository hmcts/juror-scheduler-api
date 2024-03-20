package uk.gov.hmcts.juror.scheduler.service.jobs;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestLogSpecification;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobResult;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        APIJob.class
    }
)
@DisplayName("APIJob")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods"
})
class APIJobTest {

    @MockBean
    private JobService jobService;

    @MockBean
    private EntityManager entityManager;
    @MockBean
    private EntityManagerFactory entityManagerFactory;
    @MockBean
    private PlatformTransactionManager transactionManager;

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
    private TaskEntity updatedTaskEntity;

    private static final String JOB_KEY = "ABC123";

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
        when(jobService.getJob(apiJobDetailsEntity.getKey())).thenReturn(apiJobDetailsEntity);

        when(requestSpecification.request(Method.valueOf(apiJobDetailsEntity.getMethod().name()),
            apiJobDetailsEntity.getUrl())).thenReturn(response);

        taskEntity = new TaskEntity();
        taskEntity.setJob(apiJobDetailsEntity);
        taskEntity.setTaskId(1L);
        taskEntity.setStatus(Status.PENDING);
        when(taskService.createTask(apiJobDetailsEntity)).thenReturn(taskEntity);


        updatedTaskEntity = new TaskEntity();
        when(taskService.getLatestTask(JOB_KEY, taskEntity.getTaskId())).thenReturn(updatedTaskEntity);
        when(taskService.saveTask(updatedTaskEntity)).thenReturn(updatedTaskEntity);
    }


    private void runStandardVerification(APIJobDetailsEntity apiJobDetailsEntity) {
        int defaultHeaderCount = 2;

        //Validate payload setup correctly
        if (apiJobDetailsEntity.getPayload() == null) {
            verify(requestSpecification, never()).body(anyString());
        } else {
            verify(requestSpecification, times(1)).body(apiJobDetailsEntity.getPayload());
        }

        //Validate Headers setup correctly
        if (apiJobDetailsEntity.getHeaders() == null || apiJobDetailsEntity.getHeaders().isEmpty()) {
            verify(requestSpecification, times(defaultHeaderCount)).header(anyString(), any());
        } else {
            verify(requestSpecification, times(apiJobDetailsEntity.getHeaders().size() + defaultHeaderCount)).header(
                anyString(), any());
            apiJobDetailsEntity.getHeaders()
                .forEach((key, value) -> verify(requestSpecification, times(1)).header(key, value));
        }
        verify(requestSpecification, times(1)).header("job_key", apiJobDetailsEntity.getKey());
        verify(requestSpecification, times(1)).header("task_id", taskEntity.getTaskId());

        //Validate authentication default called correctly
        if (apiJobDetailsEntity.getAuthenticationDefault() != null) {
            verify(apiJobDetailsEntity.getAuthenticationDefault(), times(1))
                .addAuthentication(apiJobDetailsEntity, requestSpecification);
        }

        //Validate validations executed correctly
        StringBuilder expectedMessageBuilder = new StringBuilder();
        AtomicBoolean passed = new AtomicBoolean(true);
        apiJobDetailsEntity.getValidations().forEach(validation -> {
            APIValidationEntity.Result result = validation.validate(response, apiJobDetailsEntity);
            if (!result.isPassed()) {
                passed.set(false);
                expectedMessageBuilder.append(validation.getType().name()).append(": ").append(result.getMessage())
                    .append('\n');
            }
        });
        assertEquals(passed.get()
                ? Status.VALIDATION_PASSED
                : Status.VALIDATION_FAILED,
            updatedTaskEntity.getStatus(), "Status must match");

        if (passed.get()) {
            assertNull(updatedTaskEntity.getMessage(), "Message should be null");
        } else {
            assertEquals(expectedMessageBuilder.toString(), updatedTaskEntity.getMessage(),
                "Message must match");
        }


        //Validate Request was sent correctly
        verify(requestSpecification, times(1))
            .request(Method.valueOf(apiJobDetailsEntity.getMethod().name()), apiJobDetailsEntity.getUrl());
        verify(taskService, times(1)).saveTask(updatedTaskEntity);
    }


    @Test
    void constructorTest() {
        APIJob apiJob = new APIJob(jobService, taskService, transactionManager);
        assertNotNull(apiJob, "APIJob must be created");
        assertThat(apiJob.jobService).isEqualTo(jobService);
        assertThat(apiJob.taskService).isEqualTo(taskService);
        assertThat(apiJob.transactionManager).isEqualTo(transactionManager);
        DefaultTransactionDefinition expectedTransactionDefinition =
            new DefaultTransactionDefinition();
        expectedTransactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
        expectedTransactionDefinition.setTimeout(-1);
        assertThat(apiJob.transactionDefinition).isEqualTo(expectedTransactionDefinition);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveTypical() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveWithPayload() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.MAX_RESPONSE_TIME));
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
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
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveWithHeaders() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.MAX_RESPONSE_TIME));

        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put("My Header", "My Header value");
        headers.put("Content-Type", "text/plain");

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
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
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveWithAuthenticationDefault() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));

        AuthenticationDefaults authenticationDefaults = mock(AuthenticationDefaults.class);

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
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
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveTaskUpdatedWhenApiRunning() {
        apiJob.entityManager = entityManager;
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));

        AuthenticationDefaults authenticationDefaults = mock(AuthenticationDefaults.class);

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .authenticationDefault(authenticationDefaults)
            .build();
        runStandardSetup(apiJobDetailsEntity);
        LocalDateTime now = LocalDateTime.now();
        taskEntity.setLastUpdatedAt(now);
        updatedTaskEntity.setLastUpdatedAt(now.plusSeconds(1));
        apiJob.execute(context);
        verify(entityManager, times(1)).detach(updatedTaskEntity);
        verify(taskService, never()).saveTask(any());
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveTaskNotUpdatedWhenApiRunning() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));

        AuthenticationDefaults authenticationDefaults = mock(AuthenticationDefaults.class);

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .authenticationDefault(authenticationDefaults)
            .build();
        runStandardSetup(apiJobDetailsEntity);
        LocalDateTime now = LocalDateTime.now();
        taskEntity.setLastUpdatedAt(now);
        updatedTaskEntity.setLastUpdatedAt(now);
        apiJob.execute(context);
        verify(entityManager, never()).detach(updatedTaskEntity);
        verify(taskService, times(1)).saveTask(any());
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void positiveTaskNotUpdatedWhenApiRunningButValidationFailed() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));

        AuthenticationDefaults authenticationDefaults = mock(AuthenticationDefaults.class);

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .authenticationDefault(authenticationDefaults)
            .build();
        runStandardSetup(apiJobDetailsEntity);
        LocalDateTime now = LocalDateTime.now();

        taskEntity.setLastUpdatedAt(now);
        updatedTaskEntity.setLastUpdatedAt(now);
        updatedTaskEntity.setStatus(Status.VALIDATION_FAILED);
        apiJob.execute(context);
        verify(entityManager, never()).detach(updatedTaskEntity);
        verify(taskService, times(1)).saveTask(any());
    }

    @Test
    void negativeKeyNotFound() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);

        apiJob.execute(context);

        ArgumentCaptor<JobResult> argumentCaptor = ArgumentCaptor.forClass(JobResult.class);
        verify(context, times(1)).setResult(argumentCaptor.capture());

        JobResult result = argumentCaptor.getValue();
        JobResult.ErrorDetails errorDetails = result.getError();

        assertFalse(result.isPassed(), "Expect result to fail");
        assertNotNull(errorDetails, "Error details should be provided");
        assertEquals("Job Key not found",
            errorDetails.getMessage(), "Message must match");
        assertInstanceOf(InternalServerException.class, errorDetails.getThrowable(), "Expect correct exception type");
    }

    @Test
    void negativeJobNotFound() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", JOB_KEY);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);
        Throwable cause = new NotFoundException("Job not found for key: " + JOB_KEY);
        when(jobService.getJob(JOB_KEY)).thenThrow(cause);


        apiJob.execute(context);

        ArgumentCaptor<JobResult> argumentCaptor = ArgumentCaptor.forClass(JobResult.class);
        verify(context, times(1)).setResult(argumentCaptor.capture());

        JobResult result = argumentCaptor.getValue();
        JobResult.ErrorDetails errorDetails = result.getError();

        assertFalse(result.isPassed(), "Expect result to fail");
        assertNotNull(errorDetails, "Error details should be provided");
        assertEquals("Job not found for key: " + JOB_KEY, errorDetails.getMessage(),
            "Expect correct message");
        assertEquals(cause, errorDetails.getThrowable(), "Expect correct throwable");

        verifyNoInteractions(taskService);
    }

    @Test
    void negativeUnexpectedExceptionBeforeTaskCreation() {
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", JOB_KEY);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(context.getJobDetail()).thenReturn(jobDetail);
        RuntimeException cause = new RuntimeException("An example unexpected error");
        when(jobService.getJob(JOB_KEY)).thenThrow(cause);

        apiJob.execute(context);

        ArgumentCaptor<JobResult> argumentCaptor = ArgumentCaptor.forClass(JobResult.class);
        verify(context, times(1)).setResult(argumentCaptor.capture());

        JobResult result = argumentCaptor.getValue();
        JobResult.ErrorDetails errorDetails = result.getError();

        assertFalse(result.isPassed(), "Expect result to fail");
        assertNotNull(errorDetails, "Error details should be provided");
        assertEquals("An example unexpected error", errorDetails.getMessage(),
            "Expect correct message");
        assertEquals(cause, errorDetails.getThrowable(), "Expect correct throwable");
    }

    @Test
    void negativeUnexpectedExceptionAfterTaskCreation() {
        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .build();
        runStandardSetup(apiJobDetailsEntity);
        RuntimeException cause = new RuntimeException("An example unexpected error");
        when(requestSpecification.request(Method.valueOf(apiJobDetailsEntity.getMethod().name()),
            apiJobDetailsEntity.getUrl())).thenThrow(cause);

        apiJob.execute(context);

        ArgumentCaptor<JobResult> argumentCaptor = ArgumentCaptor.forClass(JobResult.class);
        verify(context, times(1)).setResult(argumentCaptor.capture());

        JobResult result = argumentCaptor.getValue();
        JobResult.ErrorDetails errorDetails = result.getError();

        assertFalse(result.isPassed(), "Expect result to fail");
        assertNotNull(errorDetails, "Error details should be provided");
        assertEquals("An example unexpected error",
            errorDetails.getMessage(), "Expect correct message");
        assertEquals(cause, errorDetails.getThrowable(), "Expect correct throwable");

        assertEquals(Status.FAILED_UNEXPECTED_EXCEPTION, taskEntity.getStatus(), "Status must match");
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void negativeValidationFailedSingle() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("FAILED for XYZ").build(),
            ValidationType.STATUS_CODE));


        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
    })
    void negativeValidationFailedMultiple() {
        List<APIValidationEntity> validationEntityList = new ArrayList<>();
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(true).build(),
            ValidationType.STATUS_CODE));
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("Failed for abc").build(),
            ValidationType.MAX_RESPONSE_TIME));
        validationEntityList.add(new TestAPIJobDetailsEntity(APIValidationEntity.Result.builder().passed(false)
            .message("Failed for xyz").build(),
            ValidationType.JSON_PATH));

        APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
            .key(JOB_KEY)
            .method(APIMethod.GET)
            .url("www.myurl.com")
            .validations(validationEntityList)
            .build();
        runStandardSetup(apiJobDetailsEntity);

        apiJob.execute(context);

        runStandardVerification(apiJobDetailsEntity);
    }

    @SuppressWarnings({
        "PMD.TestClassWithoutTestCases" //False positive support class
    })
    private static class TestAPIJobDetailsEntity extends APIValidationEntity {

        private final Result result;
        private final ValidationType type;

        public TestAPIJobDetailsEntity(Result result, ValidationType type) {
            super();
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
