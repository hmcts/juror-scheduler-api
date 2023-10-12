package uk.gov.hmcts.juror.scheduler.api.controllers;


import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.testSupport.APIConstantsTest;
import uk.gov.hmcts.juror.scheduler.testSupport.TestUtil;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.testSupport.ControllerTestSupport;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.standard.service.exceptions.BusinessRuleValidationException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = JobController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
        classes = {
                JobController.class,
                ExceptionHandling.class
        }
)
@DisplayName("Controller: /job/{job-key}")
class JobControllerTest {

    private static final String CONTROLLER_BASEURL = "/job/{job-key}";
    private static final String GET_JOB_DETAILS_URL = CONTROLLER_BASEURL;
    private static final String ENABLE_JOB_URL = CONTROLLER_BASEURL + "/enable";
    private static final String DISABLE_JOB_URL = CONTROLLER_BASEURL + "/disable";
    private static final String DELETE_JOB_DETAILS_URL = CONTROLLER_BASEURL;
    private static final String RUN_JOB_URL = CONTROLLER_BASEURL + "/run";
    private static final String PATCH_JOB_DETAILS_URL = CONTROLLER_BASEURL;
    private static final String GET_JOB_TASKS = CONTROLLER_BASEURL + "/tasks";
    private static final String GET_JOB_STATUS = CONTROLLER_BASEURL + "/status";

    private static final String RESOURCE_PREFIX = "/testData/jobControllerTest";


    @MockBean
    private JobService jobService;
    @MockBean
    private TaskService taskService;
    @MockBean
    private JobDetailsMapper jobDetailsMapper;
    @MockBean
    private TaskMapper taskMapper;

    @Nested
    @DisplayName("GET " + GET_JOB_DETAILS_URL)
    class GetJobDetails extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean taskServiceCalled) throws Exception {

            this.mockMvc
                    .perform(get(GET_JOB_DETAILS_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(
                            content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true));

            if (taskServiceCalled) {
                verify(jobService, times(1)).getJob(eq(jobKey));
                verify(jobDetailsMapper, times(status == HttpStatus.NOT_FOUND ? 0 : 1)).toAPIJobDetailsResponse(any());
            } else {
                verify(jobService, never()).getJob(eq(jobKey));
                verify(jobDetailsMapper, never()).toAPIJobDetailsResponse(any());
            }
        }

        @Test
        void positive_get_job() throws Exception {
            APIJobDetailsResponse apiJobDetails = TestUtil.generateAPIJobDetailsResponse();

            final String jobKey = "ABC";
            when(jobDetailsMapper.toAPIJobDetailsResponse(any())).thenReturn(apiJobDetails);

            this.mockMvc
                    .perform(get(GET_JOB_DETAILS_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(createResponseStringFromObject(apiJobDetails), true));
            verify(jobService, times(1)).getJob(eq(jobKey));
            verify(jobDetailsMapper, times(1)).toAPIJobDetailsResponse(any());
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "getJobDetails.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .getJob(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }
    }

    @Nested
    @DisplayName("PUT " + ENABLE_JOB_URL)
    class EnableJob extends ControllerTestSupport {

        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            this.mockMvc
                    .perform(put(ENABLE_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).enable(eq(jobKey));
            } else {
                verify(jobService, never()).enable(any());
            }
        }

        @Test
        void positive_enable_job() throws Exception {
            final String jobKey = "ABC";
            this.mockMvc
                    .perform(put(ENABLE_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(Matchers.emptyOrNullString()));
            verify(jobService, times(1)).enable(eq(jobKey));
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "enableJob.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .enable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }

        @Test
        void negative_not_a_scheduled_job() throws Exception {
            final String jobKey = "ABC";
            doThrow(new BusinessRuleValidationException(new NotAScheduledJobError())).when(jobService)
                    .enable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_A_SCHEDULED_JOB", "The action you are trying to perform is only " +
                            "valid on Scheduled Jobs.",
                    HttpStatus.UNPROCESSABLE_ENTITY, true);
        }

        @Test
        void negative_already_enabled() throws Exception {
            final String jobKey = "ABC";
            doThrow(new BusinessRuleValidationException(new JobAlreadyDisabledError())).when(jobService)
                    .enable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "JOB_ALREADY_DISABLED", "This Job is already disabled",
                    HttpStatus.UNPROCESSABLE_ENTITY, true);
        }
    }

    @Nested
    @DisplayName("PUT " + DISABLE_JOB_URL)
    class DisableJob extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            this.mockMvc
                    .perform(put(DISABLE_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).disable(eq(jobKey));
            } else {
                verify(jobService, never()).disable(any());
            }
        }

        @Test
        void positive_disable_job() throws Exception {
            final String jobKey = "ABC";
            this.mockMvc
                    .perform(put(DISABLE_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(Matchers.emptyOrNullString()));
            verify(jobService, times(1)).disable(eq(jobKey));
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "disableJob.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .disable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }

        @Test
        void negative_not_a_scheduled_job() throws Exception {
            final String jobKey = "ABC";
            doThrow(new BusinessRuleValidationException(new NotAScheduledJobError())).when(jobService)
                    .disable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_A_SCHEDULED_JOB", "The action you are trying to perform is only " +
                            "valid on Scheduled Jobs.",
                    HttpStatus.UNPROCESSABLE_ENTITY, true);
        }

        @Test
        void negative_already_disabled() throws Exception {
            final String jobKey = "ABC";
            doThrow(new BusinessRuleValidationException(new JobAlreadyEnabledError())).when(jobService)
                    .disable(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "JOB_ALREADY_ENABLED", "This Job is already enabled",
                    HttpStatus.UNPROCESSABLE_ENTITY, true);
        }
    }

    @Nested
    @DisplayName("DELETE " + DELETE_JOB_DETAILS_URL)
    class DeleteJobDetails extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            this.mockMvc
                    .perform(delete(DELETE_JOB_DETAILS_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).deleteJob(eq(jobKey));
            } else {
                verify(jobService, never()).deleteJob(any());
            }
        }

        @Test
        void positive_delete_job() throws Exception {
            final String jobKey = "ABC";
            this.mockMvc
                    .perform(delete(DELETE_JOB_DETAILS_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.emptyOrNullString()));
            verify(jobService, times(1)).deleteJob(eq(jobKey));
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "deleteJob.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .deleteJob(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }
    }

    @Nested
    @DisplayName("PUT " + RUN_JOB_URL)
    class RunJob extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            this.mockMvc
                    .perform(put(RUN_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).executeJob(eq(jobKey));
            } else {
                verify(jobService, never()).executeJob(any());
            }
        }

        @Test
        void positive_run_job() throws Exception {
            final String jobKey = "ABC";
            this.mockMvc
                    .perform(put(RUN_JOB_URL, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(Matchers.emptyOrNullString()));
            verify(jobService, times(1)).executeJob(eq(jobKey));
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "runJob.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .executeJob(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }
    }

    @Nested
    @DisplayName("PATCH " + PATCH_JOB_DETAILS_URL)
    class PatchJobDetails extends ControllerTestSupport {


        protected void callAndExpectErrorResponse(
                String jobKey,
                String payload,
                String expectedErrorCode,
                String expectedErrorMessage,
                HttpStatus status,
                boolean jobServiceCalled) throws Exception {

            MockHttpServletRequestBuilder builder =
                    patch(PATCH_JOB_DETAILS_URL, jobKey).contentType(MediaType.APPLICATION_JSON);
            if (payload != null) {
                builder.content(payload);
            }
            this.mockMvc
                    .perform(builder)
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).updateJob(eq(jobKey), any());
            } else {
                verify(jobService, never()).updateJob(any(), any());
            }
        }

        public static Stream<Arguments> invalidJobPatchPayloadArgumentSource() {
            String payload = TestUtil.readResource("patchJobDetailsTypical.json", RESOURCE_PREFIX);

            return Stream.of(
                    //cron_expression
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.cron_expression", "* * * * * * *"),
                            "cronExpression: Invalid Cron Expression: Support for specifying both a day-of-week AND a "
                                    +
                                    "day-of-month parameter is not implemented."),
                    //Information.name
                    Arguments.arguments(TestUtil.deleteJsonPath(payload, "$.information.name"),
                            "information.name: must not be blank"),
                    //Information.description
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.information.description",
                                    RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_LONG)),
                            "information.description: length must be between 0 and 2500"),
                    //Information.tags
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.information.tags",
                                    new HashSet<>() {{
                                        add(RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_SHORT));
                                    }}),
                            "information.tags[]: length must be between 0 and 250"),

                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.method", "INVALID"),
                            "Invalid method entered. Allowed values are: [POST, GET, PUT, PATCH, DELETE, HEAD, "
                                    + "OPTIONS, "
                                    +
                                    "TRACE]"),
                    //URL
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.url", "INVALID"),
                            "url: must be a valid URL"),
                    //Headers
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.headers", new HashMap<>() {{
                                put("", null);
                            }}),
                            "headers[]: length must be between 1 and 2500"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.headers", new HashMap<>() {{
                                put("myKey", "");
                            }}),
                            "headers[myKey]: length must be between 1 and 2500"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.headers", new HashMap<>()),
                            "headers: size must be between 1 and 100"),
                    //authenticationDefault
                    Arguments.arguments(TestUtil.addJsonPath(payload, "$", "authentication_default", "INVALID"),
                            "Invalid authentication default entered. Allowed values are: " + Arrays.toString(
                                    AuthenticationDefaults.values())),
                    //payload
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.payload", ""),
                            "payload: length must be between 1 and 2500"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.payload", RandomStringUtils.randomAlphabetic(2501)),
                            "payload: length must be between 1 and 2500"),

                    //validations
                    Arguments.arguments(TestUtil.addJsonPath(TestUtil.deleteJsonPath(payload, "$.validations"), "$",
                                    "validations", Collections.emptyList()),
                            "validations: size must be between 1 and 250"),
                    Arguments.arguments(TestUtil.addJsonPath(TestUtil.deleteJsonPath(payload, "$.validations"), "$",
                                    "validations", new ArrayList<>() {{
                                        for (int index = 0; index <= 250; index++) {
                                            int finalIndex = index;
                                            add(new HashMap<>() {{
                                                put("type", "STATUS_CODE");
                                                put("expected_status_code", 100 + finalIndex);
                                            }});
                                        }
                                    }}),
                            "validations: size must be between 1 and 250"),

                    //Wrong type
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "STATUS_CODE");
                                put("max_response_time_ms", 1000);
                            }}),
                            "validations[0].expectedStatusCode: must not be null"),

                    //StatusCode
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "STATUS_CODE");
                                put("expected_status_code", 99);
                            }}),
                            "validations[0].expectedStatusCode: must be greater than or equal to 100"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "STATUS_CODE");
                                put("expected_status_code", 600);
                            }}),
                            "validations[0].expectedStatusCode: must be less than or equal to 599"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "STATUS_CODE");
                            }}),
                            "validations[0].expectedStatusCode: must not be null"),
                    //Max ResponseTime
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "MAX_RESPONSE_TIME");
                            }}),
                            "validations[0].maxResponseTimeMS: must not be null"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "MAX_RESPONSE_TIME");
                                put("max_response_time_ms", 0);
                            }}),
                            "validations[0].maxResponseTimeMS: must be greater than or equal to 1"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "MAX_RESPONSE_TIME");
                                put("max_response_time_ms", 30001);
                            }}),
                            "validations[0].maxResponseTimeMS: must be less than or equal to 30000"),
                    //JsonPath
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "JSON_PATH");
                                put("path", "$.status");
                            }}),
                            "validations[0].expectedResponse: must not be null"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "JSON_PATH");
                                put("expected_response", "UP");
                            }}),
                            "validations[0].path: must not be null"),
                    Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.validations[0]", new HashMap<>() {{
                                put("type", "JSON_PATH");
                                put("path", "$[[$");
                                put("expected_response", "UP");
                            }}),
                            "validations[0].path: Invalid JsonPath")
            );
        }

        @ParameterizedTest(name = "Expect error message: {1}")
        @MethodSource("invalidJobPatchPayloadArgumentSource")
        void negative_patch_api_job_invalid_payload(String payload, String expectedErrorMessage) throws Exception {
            callAndExpectErrorResponse("ABC", payload, "INVALID_PAYLOAD", expectedErrorMessage,
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_invalid_key() throws Exception {
            String payload = TestUtil.readResource("patchJobDetailsTypical.json", RESOURCE_PREFIX);

            callAndExpectErrorResponse("A", payload, "INVALID_PAYLOAD", "updateJob.jobKey: must match \\\"[A-Z_]{3," +
                            "50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_no_payload() throws Exception {
            callAndExpectErrorResponse("A", null, "INVALID_PAYLOAD", "Unable to read payload content",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            String payload = TestUtil.readResource("patchJobDetailsTypical.json", RESOURCE_PREFIX);

            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(jobService)
                    .updateJob(eq(jobKey),
                            any());
            callAndExpectErrorResponse(jobKey, payload, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }

        @Test
        void positive_all_fields() throws Exception {
            String payload = TestUtil.readResource("patchJobDetailsTypical.json", RESOURCE_PREFIX);

            APIJobDetailsResponse response = TestUtil.generateAPIJobDetailsResponse();

            when(jobDetailsMapper.toAPIJobDetailsResponse(any())).thenReturn(response);

            final String jobKey = "ABC";

            this.mockMvc
                    .perform(patch(PATCH_JOB_DETAILS_URL, jobKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(createResponseStringFromObject(response), true));
            verify(jobDetailsMapper, times(1)).toAPIJobDetailsResponse(any());

            final ArgumentCaptor<APIJobPatch> captor = ArgumentCaptor.forClass(APIJobPatch.class);
            verify(jobService, times(1)).updateJob(eq(jobKey), captor.capture());
            final APIJobPatch apiJobPatch = captor.getValue();

            assertEquals("* 5 * * * ?", apiJobPatch.getCronExpression());
            assertNotNull(apiJobPatch.getInformation());
            Information information = apiJobPatch.getInformation();
            assertEquals("name 123", information.getName());
            assertEquals("description 123", information.getDescription());
            assertEquals(1, information.getTags().size());
            assertEquals("tag 123", information.getTags().stream().findFirst().get());

            Assertions.assertEquals(APIMethod.POST, apiJobPatch.getMethod());
            assertEquals("http://localhost:8080/health", apiJobPatch.getUrl());
            Map<String, String> headers = apiJobPatch.getHeaders();
            assertNotNull(headers);
            assertEquals(3, headers.size());
            assertEquals("val1", headers.get("additionalProp1"));
            assertEquals("value2", headers.get("additionalProp2"));
            assertEquals("value 3", headers.get("additionalProp3"));
            Assertions.assertEquals(AuthenticationDefaults.NONE, apiJobPatch.getAuthenticationDefault());
            assertEquals(("{\"value\":\"key1\",\"value2\":\"key2\",\"value3\":\"key3\"," +
                    "\"value4\":{\"value1\":\"key1\",\"value2\":\"key2\"}}"), apiJobPatch.getPayload());
            //Validations
            assertEquals(3, apiJobPatch.getValidations().size());
            assertInstanceOf(StatusCodeAPIValidation.class, apiJobPatch.getValidations().get(0));
            StatusCodeAPIValidation statusCodeAPIValidation =
                    (StatusCodeAPIValidation) apiJobPatch.getValidations().get(0);
            assertEquals(ValidationType.STATUS_CODE, statusCodeAPIValidation.getType());
            assertEquals(201, statusCodeAPIValidation.getExpectedStatusCode());

            assertInstanceOf(MaxResponseTimeAPIValidation.class, apiJobPatch.getValidations().get(1));
            MaxResponseTimeAPIValidation maxResponseTimeAPIValidation =
                    (MaxResponseTimeAPIValidation) apiJobPatch.getValidations().get(1);
            Assertions.assertEquals(ValidationType.MAX_RESPONSE_TIME, maxResponseTimeAPIValidation.getType());
            assertEquals(1000, maxResponseTimeAPIValidation.getMaxResponseTimeMS());

            assertInstanceOf(JsonPathAPIValidation.class, apiJobPatch.getValidations().get(2));
            JsonPathAPIValidation jsonPathAPIValidation = (JsonPathAPIValidation) apiJobPatch.getValidations().get(2);
            Assertions.assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidation.getType());
            assertEquals("status", jsonPathAPIValidation.getPath());
            assertEquals("UP", jsonPathAPIValidation.getExpectedResponse());
        }

        @Test
        void positive_one_field() throws Exception {
            String payload = TestUtil.readResource("patchJobDetailsSingleField.json", RESOURCE_PREFIX);

            APIJobDetailsResponse response = TestUtil.generateAPIJobDetailsResponse();

            when(jobDetailsMapper.toAPIJobDetailsResponse(any())).thenReturn(response);

            final String jobKey = "ABC";

            this.mockMvc
                    .perform(patch(PATCH_JOB_DETAILS_URL, jobKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(createResponseStringFromObject(response), true));
            verify(jobDetailsMapper, times(1)).toAPIJobDetailsResponse(any());

            final ArgumentCaptor<APIJobPatch> captor = ArgumentCaptor.forClass(APIJobPatch.class);
            verify(jobService, times(1)).updateJob(eq(jobKey), captor.capture());
            final APIJobPatch apiJobPatch = captor.getValue();

            assertEquals("8 7 * * * ?", apiJobPatch.getCronExpression());
            assertNull(apiJobPatch.getInformation());
            assertNull(apiJobPatch.getMethod());
            assertNull(apiJobPatch.getUrl());
            assertNull(apiJobPatch.getHeaders());
            assertNull(apiJobPatch.getAuthenticationDefault());
            assertNull(apiJobPatch.getPayload());
            assertNull(apiJobPatch.getValidations());
        }
    }

    @Nested
    @DisplayName("GET " + GET_JOB_TASKS)
    class GetJobTasks extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean taskServiceCalled) throws Exception {

            this.mockMvc
                    .perform(get(GET_JOB_TASKS, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (taskServiceCalled) {
                verify(taskService, times(1)).getTasks(eq(jobKey));
                verify(taskMapper, times(status == HttpStatus.NOT_FOUND ? 0 : 1)).toTaskList(any());
            } else {
                verify(taskService, never()).getTasks(eq(jobKey));
                verify(taskMapper, never()).toTaskList(any());
            }
        }

        private void performAndValidatePositive(List<TaskDetail> taskDetails) throws Exception {
            final String jobKey = "ABC";
            when(taskMapper.toTaskList(any())).thenReturn(taskDetails);

            this.mockMvc
                    .perform(get(GET_JOB_TASKS, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(createResponseStringFromObject(taskDetails), true));
            verify(taskService, times(1)).getTasks(eq(jobKey));
            verify(taskMapper, times(1)).toTaskList(any());
        }

        @Test
        void positive_get_tasks_single() throws Exception {
            List<TaskDetail> responseTaskDetails = new ArrayList<>();
            responseTaskDetails.add(TestUtil.generateTask());
            performAndValidatePositive(responseTaskDetails);
        }

        @Test
        void positive_get_tasks_multiple() throws Exception {
            List<TaskDetail> responseTaskDetails = new ArrayList<>();
            responseTaskDetails.add(TestUtil.generateTask());
            responseTaskDetails.add(TestUtil.generateTask());
            responseTaskDetails.add(TestUtil.generateTask());
            performAndValidatePositive(responseTaskDetails);
        }

        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "getJobTasks.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(taskService)
                    .getTasks(eq(jobKey));
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }
    }

    @Nested
    @DisplayName("GET " + GET_JOB_STATUS)
    class GetJobStatus extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean taskServiceCalled) throws Exception {

            this.mockMvc
                    .perform(get(GET_JOB_STATUS, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().is(status.value()))
                    .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (taskServiceCalled) {
                verify(taskService, times(1)).getLatestTask(jobKey);
                verify(taskMapper, times(status == HttpStatus.NOT_FOUND ? 0 : 1)).toTask(any());
            } else {
                verify(taskService, never()).getTasks(jobKey);
                verify(taskMapper, never()).toTask(any());
            }
        }

        private void performAndValidatePositive(TaskDetail taskDetail) throws Exception {
            final String jobKey = "ABC";
            when(taskMapper.toTask(any())).thenReturn(taskDetail);

            this.mockMvc
                    .perform(get(GET_JOB_STATUS, jobKey).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().json(createResponseStringFromObject(taskDetail), true));
            verify(taskService, times(1)).getLatestTask(jobKey);
            verify(taskMapper, times(1)).toTask(any());
        }

        @Test
        void positive_get_task() throws Exception {
            performAndValidatePositive(TestUtil.generateTask());
        }
        @Test
        void negative_invalid_key() throws Exception {
            callAndExpectErrorResponse("A", "INVALID_PAYLOAD", "getJobStatus.jobKey: must match \\\"[A-Z_]{3,50}\\\"",
                    HttpStatus.BAD_REQUEST, false);
        }

        @Test
        void negative_not_found() throws Exception {
            final String jobKey = "ABC";
            doThrow(new NotFoundException("Job with key '" + jobKey + "' not found")).when(taskService)
                    .getLatestTask(jobKey);
            callAndExpectErrorResponse(jobKey, "NOT_FOUND", "The requested resource could not be " +
                            "located.",
                    HttpStatus.NOT_FOUND, true);
        }
    }
}
