package uk.gov.hmcts.juror.scheduler.api.controllers;

import org.apache.commons.lang3.RandomStringUtils;
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
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.testSupport.APIConstantsTest;
import uk.gov.hmcts.juror.scheduler.testSupport.ControllerTestSupport;
import uk.gov.hmcts.juror.scheduler.testSupport.TestUtil;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.GenericErrorHandlerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = JobsController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
    classes = {
        JobsController.class,
        ExceptionHandling.class
    }
)
@DisplayName("Controller:  /jobs")
class JobsControllerTest {

    private static final String CONTROLLER_BASEURL = "/jobs";
    private static final String CREATE_API_JOB_URL = CONTROLLER_BASEURL + "/api";
    private static final String SEARCH_API_JOB_URL = CONTROLLER_BASEURL + "/search";

    private static final String RESOURCE_PREFIX = "/testData/jobsController";

    @MockBean
    private JobService jobService;

    @MockBean
    private JobDetailsMapper jobDetailsMapper;


    @Nested
    @DisplayName("POST " + CREATE_API_JOB_URL)
    class CreateAPI extends ControllerTestSupport {


        protected void callAndExpectErrorResponse(String payload,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            MockHttpServletRequestBuilder builder = post(CREATE_API_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            if (payload != null){
                builder.content(payload);
            }
            this.mockMvc
                .perform(builder)
                .andDo(print())
                .andExpect(status().is(status.value()))
                .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).createJob(any());
            } else {
                verify(jobService, never()).createJob(any());
            }
        }


        public static Stream<Arguments> invalidPayloadArgumentSource() {
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);

            return Stream.of(
                //type
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.type", "INVALID"),
                    "Invalid job type entered. Allowed values are: [API]"),
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.type"),
                    "type: must not be null"),

                //cronExpression
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.cron_expression", "* * * * * * *"),
                    "cronExpression: Invalid Cron Expression: Support for specifying both a day-of-week AND a " +
                        "day-of-month parameter is not implemented."),

                //key
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.key", ""),
                    "key: must match \\\"[A-Z_]{3,50}\\\""),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.key", "-_':"),
                    "key: must match \\\"[A-Z_]{3,50}\\\""),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.key", "AB"),
                    "key: must match \\\"[A-Z_]{3,50}\\\""),
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.key"),
                    "key: must not be null"),

                //information
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.information"),
                    "information: must not be null"),
                //Information.name
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.information.name"),
                    "information.name: must not be blank"),
                //Information.description
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.information.description",
                        RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_LONG)),
                    "information.description: length must be between 0 and 2500"),
                //Information.tags
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.information.tags",
                        new HashSet<>() {{
                            add(RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_SHORT));
                        }}),
                    "information.tags[]: length must be between 0 and 250"),

                //Method
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.method", "INVALID"),
                    "Invalid method entered. Allowed values are: [POST, GET, PUT, PATCH, DELETE, HEAD, OPTIONS, " +
                        "TRACE]"),
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.method"),
                    "method: must not be null"),
                //URL
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.url", "INVALID"),
                    "url: must be a valid URL"),
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.url"),
                    "url: must not be null"),
                //Headers
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", new HashMap<>() {{
                        put("", null);
                    }}),
                    "headers[]: length must be between 1 and 2500"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", new HashMap<>() {{
                        put("myKey", "");
                    }}),
                    "headers[myKey]: length must be between 1 and 2500"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", new HashMap<>()),
                    "headers: size must be between 1 and 100"),
                //authenticationDefault
                Arguments.arguments(TestUtil.addJsonPath(postPayload, "$", "authentication_default", "INVALID"),
                    "Invalid authentication default entered. Allowed values are: "+Arrays.toString(
                            AuthenticationDefaults.values())),
                //payload
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.payload", ""),
                    "payload: length must be between 1 and 2500"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.payload", RandomStringUtils.randomAlphabetic(2501)),
                    "payload: length must be between 1 and 2500"),

                //validations
                Arguments.arguments(TestUtil.deleteJsonPath(postPayload, "$.validations"),
                    "validations: must not be empty"),
                Arguments.arguments(TestUtil.addJsonPath(TestUtil.deleteJsonPath(postPayload, "$.validations"),"$",
                        "validations", new ArrayList<>(){{
                            for(int index = 0; index <= 250;index++){
                                int finalIndex = index;
                                add(new HashMap<>() {{
                                    put("type", "STATUS_CODE");
                                    put("expected_status_code", 100 + finalIndex);
                                }});
                            }
                        }}),
                    "validations: size must be between 1 and 250"),

                //Wrong type
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "STATUS_CODE");
                        put("max_response_time_ms", 1000);
                    }}),
                    "validations[0].expectedStatusCode: must not be null"),

                //StatusCode
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "STATUS_CODE");
                        put("expected_status_code", 99);
                    }}),
                    "validations[0].expectedStatusCode: must be greater than or equal to 100"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "STATUS_CODE");
                        put("expected_status_code", 600);
                    }}),
                    "validations[0].expectedStatusCode: must be less than or equal to 599"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "STATUS_CODE");
                    }}),
                    "validations[0].expectedStatusCode: must not be null"),
                //Max ResponseTime
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "MAX_RESPONSE_TIME");
                    }}),
                    "validations[0].maxResponseTimeMS: must not be null"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "MAX_RESPONSE_TIME");
                        put("max_response_time_ms", 0);
                    }}),
                    "validations[0].maxResponseTimeMS: must be greater than or equal to 1"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "MAX_RESPONSE_TIME");
                        put("max_response_time_ms", 30001);
                    }}),
                    "validations[0].maxResponseTimeMS: must be less than or equal to 30000"),
                //JsonPath
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "JSON_PATH");
                        put("path", "$.status");
                    }}),
                    "validations[0].expectedResponse: must not be null"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "JSON_PATH");
                        put("expected_response", "UP");
                    }}),
                    "validations[0].path: must not be null"),
                Arguments.arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]", new HashMap<>() {{
                        put("type", "JSON_PATH");
                        put("path", "$[[$");
                        put("expected_response", "UP");
                    }}),
                    "validations[0].path: Invalid JsonPath")
            );
        }


        public static Stream<Arguments> validPayloadArgumentSource() {
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);

            return Stream.of(
                Arguments.arguments("Null Header Key", TestUtil.replaceJsonPath(postPayload, "$.headers", new HashMap<>() {{
                    put("myKey", null);
                }})),
                Arguments.arguments("With Authentication Default", TestUtil.addJsonPath(postPayload, "$",
                    "authentication_default",
                    AuthenticationDefaults.JUROR_API_SERVICE))
            );
        }


        @ParameterizedTest(name = ": {0}")
        @MethodSource("validPayloadArgumentSource")
        void positive_create_api_job_valid_payload(String testName, String payload) throws Exception {

            this.mockMvc
                .perform(
                    post(CREATE_API_JOB_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                )
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
            verify(jobService, times(1)).createJob(any());
        }

        @ParameterizedTest(name = "Expect error message: {1}")
        @MethodSource("invalidPayloadArgumentSource")
        void negative_create_api_job_invalid_payload(String payload, String expectedErrorMessage) throws Exception {
            callAndExpectErrorResponse(payload, "INVALID_PAYLOAD", expectedErrorMessage, HttpStatus.BAD_REQUEST, false);
        }
        @Test
        void negative_no_payload() throws Exception {
            callAndExpectErrorResponse(null ,"INVALID_PAYLOAD", "Unable to read payload content",
                HttpStatus.BAD_REQUEST, false);
        }


        @Test
        void negative_create_api_job_key_already_in_use() throws Exception {
            doThrow(new GenericErrorHandlerException(APIHandleableException.Type.INFORMATIONAL,
                new KeyAlreadyInUseError(), HttpStatus.CONFLICT))
                .when(jobService).createJob(any());
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);

            callAndExpectErrorResponse(postPayload, "KEY_ALREADY_IN_USE", "The key you have provided is already in " +
                "use. Please choice a unique key.", HttpStatus.CONFLICT, true);
        }

        @Test
        void positive_create_api_job_get() throws Exception {
            this.mockMvc
                .perform(
                    post(CREATE_API_JOB_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.readResource("createAPIJobTypicalGET.json", RESOURCE_PREFIX))
                )
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
            verify(jobService, times(1)).createJob(any());

            final ArgumentCaptor<APIJobDetails> captor = ArgumentCaptor.forClass(APIJobDetails.class);
            verify(jobService).createJob(captor.capture());


            final APIJobDetails apiJobDetails = captor.getValue();

            Assertions.assertEquals("* 5 * * * ?", apiJobDetails.getCronExpression());


            Assertions.assertEquals("HEALTH", apiJobDetails.getKey());
            Assertions.assertEquals(APIMethod.GET, apiJobDetails.getMethod());
            Assertions.assertEquals(JobType.API, apiJobDetails.getType());
            assertEquals("http://localhost:8080/health", apiJobDetails.getUrl());
            validateDefaultCreateJob(apiJobDetails);
            assertNull(apiJobDetails.getPayload());
        }

        @Test
        void positive_create_api_job_post() throws Exception {
            this.mockMvc
                .perform(
                    post(CREATE_API_JOB_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX))
                )
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
            verify(jobService, times(1)).createJob(any());

            final ArgumentCaptor<APIJobDetails> captor = ArgumentCaptor.forClass(APIJobDetails.class);
            verify(jobService).createJob(captor.capture());


            final APIJobDetails apiJobDetails = captor.getValue();

            Assertions.assertEquals("* 5 * * * ?", apiJobDetails.getCronExpression());


            Assertions.assertEquals("HEALTH_CHECK", apiJobDetails.getKey());
            Assertions.assertEquals(APIMethod.POST, apiJobDetails.getMethod());
            Assertions.assertEquals(JobType.API, apiJobDetails.getType());
            assertEquals("http://localhost:8080/health", apiJobDetails.getUrl());
            validateDefaultCreateJob(apiJobDetails);
            assertEquals("{\"value\":\"key1\",\"value2\":\"key2\",\"value3\":\"key3\"," +
                "\"value4\":{\"value1\":\"key1\"," +
                "\"value2\":\"key2\"}}", apiJobDetails.getPayload());
        }

        private void validateDefaultCreateJob(APIJobDetails apiJobDetails) {
            Information information = apiJobDetails.getInformation();
            assertNotNull(information);
            assertEquals("Health Check", information.getName());
            assertEquals("Checks to ensure the health of the application is okay", information.getDescription());
            assertThat(information.getTags(), hasItems("Health Check"));
            assertEquals(1, information.getTags().size());
            //Headers

            assertEquals(2, apiJobDetails.getHeaders().size());
            assertEquals("testHeaderValue", apiJobDetails.getHeaders().get("testHeader"));
            assertEquals("testHeaderValue2", apiJobDetails.getHeaders().get("testHeader2"));
            //Validations
            assertEquals(6, apiJobDetails.getValidations().size());
            assertInstanceOf(StatusCodeAPIValidation.class, apiJobDetails.getValidations().get(0));
            StatusCodeAPIValidation statusCodeAPIValidation =
                (StatusCodeAPIValidation) apiJobDetails.getValidations().get(0);
            Assertions.assertEquals(ValidationType.STATUS_CODE, statusCodeAPIValidation.getType());
            assertEquals(201, statusCodeAPIValidation.getExpectedStatusCode());

            assertInstanceOf(MaxResponseTimeAPIValidation.class, apiJobDetails.getValidations().get(1));
            MaxResponseTimeAPIValidation maxResponseTimeAPIValidation =
                (MaxResponseTimeAPIValidation) apiJobDetails.getValidations().get(1);
            Assertions.assertEquals(ValidationType.MAX_RESPONSE_TIME, maxResponseTimeAPIValidation.getType());
            assertEquals(1000, maxResponseTimeAPIValidation.getMaxResponseTimeMS());

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(2));
            JsonPathAPIValidation jsonPathAPIValidation = (JsonPathAPIValidation) apiJobDetails.getValidations().get(2);
            Assertions.assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidation.getType());
            assertEquals("status", jsonPathAPIValidation.getPath());
            assertEquals("UP", jsonPathAPIValidation.getExpectedResponse());

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(3));
            JsonPathAPIValidation jsonPathAPIValidationDB =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(3);
            Assertions.assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationDB.getType());
            assertEquals("components.db.status", jsonPathAPIValidationDB.getPath());
            assertEquals("UP", jsonPathAPIValidationDB.getExpectedResponse());

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(4));
            JsonPathAPIValidation jsonPathAPIValidationDiskSpace =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(4);
            Assertions.assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationDiskSpace.getType());
            assertEquals("components.diskspace.status", jsonPathAPIValidationDiskSpace.getPath());
            assertEquals("UP", jsonPathAPIValidationDiskSpace.getExpectedResponse());

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(5));
            JsonPathAPIValidation jsonPathAPIValidationPing =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(5);
            Assertions.assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationPing.getType());
            assertEquals("components.ping.status", jsonPathAPIValidationPing.getPath());
            assertEquals("UP", jsonPathAPIValidationPing.getExpectedResponse());
        }
    }

    @Nested
    @DisplayName("GET " + SEARCH_API_JOB_URL)
    class SearchJob extends ControllerTestSupport {



        protected void callAndExpectInvalidPayloadErrorResponse(Map<String, String[]> queryParams,
                                                                String expectedErrorMessage) throws Exception {
            callAndExpectErrorResponse(queryParams, "INVALID_PAYLOAD", expectedErrorMessage, HttpStatus.BAD_REQUEST,
                false);
        }

        protected void callAndExpectErrorResponse(Map<String, String[]> queryParams,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            MockHttpServletRequestBuilder requestBuilder = get(SEARCH_API_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                requestBuilder.queryParam(entry.getKey(), entry.getValue());
            }

            this.mockMvc
                .perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(status.value()))
                .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(jobService, times(1)).getJobs(any());
                verify(jobDetailsMapper, times(status == HttpStatus.NOT_FOUND ? 0 : 1)).toJobDetailsJobDetailsList(any());
            } else {
                verify(jobService, never()).getJobs(any());
                verify(jobDetailsMapper, never()).toJobDetailsJobDetailsList(any());
            }
        }

        protected void callAndExpectValidResponse(Map<String, String[]> queryParams) throws Exception {

            MockHttpServletRequestBuilder requestBuilder = get(SEARCH_API_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                requestBuilder.queryParam(entry.getKey(), entry.getValue());
            }


            List<APIJobDetailsResponse> responseList = new ArrayList<>();

            responseList.add(TestUtil.generateAPIJobDetailsResponse());
            responseList.add(TestUtil.generateAPIJobDetailsResponse());
            responseList.add(TestUtil.generateAPIJobDetailsResponse());

            when(jobDetailsMapper.toJobDetailsJobDetailsList(any())).thenReturn(responseList);

            this.mockMvc
                .perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().json(createResponseStringFromObject(responseList),
                    true));

            verify(jobDetailsMapper, times(1)).toJobDetailsJobDetailsList(any());

            final ArgumentCaptor<JobSearchFilter> captor = ArgumentCaptor.forClass(JobSearchFilter.class);
            verify(jobService, times(1)).getJobs(captor.capture());

            final JobSearchFilter jobSearchFilter = captor.getValue();

            if (queryParams.containsKey("job_key")) {
                assertEquals(queryParams.get("job_key")[0], jobSearchFilter.getJobKey());
            } else {
                assertNull(jobSearchFilter.getJobKey());
            }

            if (queryParams.containsKey("tag")) {
                String[] expectedTags = queryParams.get("tag");
                Set<String> actualTags = jobSearchFilter.getTags();
                assertEquals(expectedTags.length, actualTags.size());
                assertThat(actualTags, hasItems(expectedTags));
            } else {
                assertNull(jobSearchFilter.getTags());
            }
        }

        @Test
        void negative_not_found() throws Exception {
            when(jobService.getJobs(any())).thenThrow(new NotFoundException("No Jobs found for the provided filter"));
            callAndExpectErrorResponse(Collections.emptyMap(), "NOT_FOUND", "The requested resource could not be " +
                    "located.",
                HttpStatus.NOT_FOUND, true);
        }

        @Test
        void negative_invalid_job_key() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(new HashMap<>() {{
                put("job_key", new String[]{"IN"});
            }}, "getJobs.jobKey: must match \\\"[A-Z_]{3,50}\\\"");
        }

        @Test
        void negative_too_long_tags() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(new HashMap<>() {{
                put("tag", new String[]{RandomStringUtils.random(APIConstantsTest.DEFAULT_MAX_LENGTH_SHORT)});
            }}, "getJobs.tags[].<iterable element>: length must be between 0 and 250");
        }

        @Test
        void negative_blank_tags() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(new HashMap<>() {{
                put("tag", new String[]{" "});
            }}, "getJobs.tags[].<iterable element>: must not be blank");
        }


        @Test
        void positive_one_tag() throws Exception {
            callAndExpectValidResponse(new HashMap<>() {{
                put("tag", new String[]{"ValidTag"});
            }});
        }

        @Test
        void positive_multiple_tag() throws Exception {
            callAndExpectValidResponse(new HashMap<>() {{
                put("tag", new String[]{"ValidTag", "ValidTag2", "ValidTag3", "ValidTag4"});
            }});
        }

        @Test
        void positive_job_key() throws Exception {
            callAndExpectValidResponse(new HashMap<>() {{
                put("job_key", new String[]{"ABC"});
            }});
        }

        @Test
        void positive_job_key_with_tags() throws Exception {
            callAndExpectValidResponse(new HashMap<>() {{
                put("job_key", new String[]{"ABC"});
                put("tag", new String[]{"ValidTag", "ValidTag2", "ValidTag3", "ValidTag4"});
            }});
        }
    }
}
