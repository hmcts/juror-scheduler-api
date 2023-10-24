package uk.gov.hmcts.juror.scheduler.api.controllers;

import org.apache.commons.lang3.RandomStringUtils;
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
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
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
import uk.gov.hmcts.juror.scheduler.testsupport.APIConstantsTest;
import uk.gov.hmcts.juror.scheduler.testsupport.ControllerTestSupport;
import uk.gov.hmcts.juror.scheduler.testsupport.util.GenerateUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.TestUtil;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.GenericErrorHandlerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter",
    "PMD.TooManyMethods"
})
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
                verify(jobService, times(1)).createJob(any());
            } else {
                verify(jobService, never()).createJob(any());
            }
        }


        public static Stream<Arguments> invalidPayloadArgumentSource() {
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);

            ArrayList<Map<String, Object>> tooManyValidationsList = new ArrayList<>();
            for (int index = 0; index <= 250; index++) {
                tooManyValidationsList.add(Map.of("type", "STATUS_CODE",
                    "expected_status_code", 100 + index));
            }

            HashMap<String, Object> blankKey = new HashMap<>();
            blankKey.put("", null);

            return Stream.of(
                //type
                arguments(TestUtil.replaceJsonPath(postPayload, "$.type", "INVALID"),
                    "Invalid job type entered. Allowed values are: [API]"),
                arguments(TestUtil.deleteJsonPath(postPayload, "$.type"),
                    "type: must not be null"),

                //cronExpression
                arguments(TestUtil.replaceJsonPath(postPayload, "$.cron_expression", "* * * * * * *"),
                    "cronExpression: Invalid Cron Expression: Support for specifying both a day-of-week AND a "
                        + "day-of-month parameter is not implemented."),

                //key
                arguments(TestUtil.replaceJsonPath(postPayload, "$.key", ""),
                    "key: must match \\\"[A-Z_0-9]{3,50}\\\""),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.key", "-_':"),
                    "key: must match \\\"[A-Z_0-9]{3,50}\\\""),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.key", "AB"),
                    "key: must match \\\"[A-Z_0-9]{3,50}\\\""),
                arguments(TestUtil.deleteJsonPath(postPayload, "$.key"),
                    "key: must not be null"),

                //information
                arguments(TestUtil.deleteJsonPath(postPayload, "$.information"),
                    "information: must not be null"),
                //Information.name
                arguments(TestUtil.deleteJsonPath(postPayload, "$.information.name"),
                    "information.name: must not be blank"),
                //Information.description
                arguments(TestUtil.replaceJsonPath(postPayload, "$.information.description",
                        RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_LONG)),
                    "information.description: length must be between 0 and 2500"),
                //Information.tags
                arguments(TestUtil.replaceJsonPath(postPayload, "$.information.tags",
                        Set.of(RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_SHORT))),
                    "information.tags[]: length must be between 0 and 250"),

                //Method
                arguments(TestUtil.replaceJsonPath(postPayload, "$.method", "INVALID"),
                    "Invalid method entered. Allowed values are: [POST, GET, PUT, PATCH, DELETE, HEAD, OPTIONS, "
                        + "TRACE]"),
                arguments(TestUtil.deleteJsonPath(postPayload, "$.method"),
                    "method: must not be null"),
                //URL
                arguments(TestUtil.replaceJsonPath(postPayload, "$.url", "INVALID"),
                    "url: must be a valid URL"),
                arguments(TestUtil.deleteJsonPath(postPayload, "$.url"),
                    "url: must not be null"),
                //Headers
                arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", blankKey),
                    "headers[]: length must be between 1 and 2500"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", Map.of("myKey", "")),
                    "headers[myKey]: length must be between 1 and 2500"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.headers", new HashMap<>()),
                    "headers: size must be between 1 and 100"),
                //authenticationDefault
                arguments(TestUtil.addJsonPath(postPayload, "$", "authentication_default", "INVALID"),
                    "Invalid authentication default entered. Allowed values are: " + Arrays.toString(
                        AuthenticationDefaults.values())),
                //payload
                arguments(TestUtil.replaceJsonPath(postPayload, "$.payload", ""),
                    "payload: length must be between 1 and 2500"),
                arguments(
                    TestUtil.replaceJsonPath(postPayload, "$.payload", RandomStringUtils.randomAlphabetic(2501)),
                    "payload: length must be between 1 and 2500"),

                //validations
                arguments(TestUtil.deleteJsonPath(postPayload, "$.validations"),
                    "validations: must not be empty"),
                arguments(TestUtil.addJsonPath(TestUtil.deleteJsonPath(postPayload, "$.validations"), "$",
                        "validations", tooManyValidationsList),
                    "validations: size must be between 1 and 250"),

                //Wrong type
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "STATUS_CODE",
                            "max_response_time_ms", 1000)),
                    "validations[0].expectedStatusCode: must not be null"),

                //StatusCode
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "STATUS_CODE",
                            "expected_status_code", 99)),
                    "validations[0].expectedStatusCode: must be greater than or equal to 100"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "STATUS_CODE",
                            "expected_status_code", 600)),
                    "validations[0].expectedStatusCode: must be less than or equal to 599"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "STATUS_CODE")),
                    "validations[0].expectedStatusCode: must not be null"),
                //Max ResponseTime
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "MAX_RESPONSE_TIME")),
                    "validations[0].maxResponseTimeMS: must not be null"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "MAX_RESPONSE_TIME",
                            "max_response_time_ms", 0)),
                    "validations[0].maxResponseTimeMS: must be greater than or equal to 1"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "MAX_RESPONSE_TIME",
                            "max_response_time_ms", 30_001)),
                    "validations[0].maxResponseTimeMS: must be less than or equal to 30000"),
                //JsonPath
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "JSON_PATH",
                            "path", "$.status")),
                    "validations[0].expectedResponse: must not be null"),
                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "JSON_PATH",
                            "expected_response", "UP")),
                    "validations[0].path: must not be null"),

                arguments(TestUtil.replaceJsonPath(postPayload, "$.validations[0]",
                        Map.of("type", "JSON_PATH",
                            "path", "$[[$",
                            "expected_response", "UP")),
                    "validations[0].path: Invalid JsonPath")
            );
        }


        public static Stream<Arguments> validPayloadArgumentSource() {
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);
            HashMap<String, Object> headerMapNullValue = new HashMap<>();
            headerMapNullValue.put("myKey", null);
            return Stream.of(
                arguments("Null Header Key",
                    TestUtil.replaceJsonPath(postPayload, "$.headers", headerMapNullValue)),
                arguments("With Authentication Default", TestUtil.addJsonPath(postPayload, "$",
                    "authentication_default",
                    AuthenticationDefaults.JUROR_API_SERVICE))
            );
        }


        @ParameterizedTest(name = ": {0}")
        @MethodSource("validPayloadArgumentSource")
        void positiveCreateApiJobValidPayload(String testName, String payload) throws Exception {
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
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeCreateApiJobInvalidPayload(String payload, String expectedErrorMessage) throws Exception {
            callAndExpectErrorResponse(payload, "INVALID_PAYLOAD", expectedErrorMessage, HttpStatus.BAD_REQUEST, false);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeNoPayload() throws Exception {
            callAndExpectErrorResponse(null, "INVALID_PAYLOAD", "Unable to read payload content",
                HttpStatus.BAD_REQUEST, false);
        }


        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeCreateApiJobKeyAlreadyInUse() throws Exception {
            doThrow(new GenericErrorHandlerException(APIHandleableException.Type.INFORMATIONAL,
                new KeyAlreadyInUseError(), HttpStatus.CONFLICT))
                .when(jobService).createJob(any());
            String postPayload = TestUtil.readResource("createAPIJobTypicalPOST.json", RESOURCE_PREFIX);

            callAndExpectErrorResponse(postPayload, "KEY_ALREADY_IN_USE",
                "The key you have provided is already in use. Please choice a unique key.",
                HttpStatus.CONFLICT, true);
        }

        @Test
        void positiveCreateApiJobGet() throws Exception {
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

            assertEquals("* 5 * * * ?", apiJobDetails.getCronExpression(),"Cron expression must match");


            assertEquals("HEALTH", apiJobDetails.getKey(),"Key must match");
            assertEquals(APIMethod.GET, apiJobDetails.getMethod(),"Method must match");
            assertEquals(JobType.API, apiJobDetails.getType(),"Type must match");
            assertEquals("http://localhost:8080/health", apiJobDetails.getUrl(),"Url must match");
            validateDefaultCreateJob(apiJobDetails);
            assertNull(apiJobDetails.getPayload(),"Payload must be null");
        }

        @Test
        void positiveCreateApiJobPost() throws Exception {
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

            assertEquals("* 5 * * * ?", apiJobDetails.getCronExpression(),"Cron expression must match");


            assertEquals("HEALTH_CHECK", apiJobDetails.getKey(),"Key must match");
            assertEquals(APIMethod.POST, apiJobDetails.getMethod(),"Method must match");
            assertEquals(JobType.API, apiJobDetails.getType(),"Type must match");
            assertEquals("http://localhost:8080/health", apiJobDetails.getUrl(),"Url must match");
            validateDefaultCreateJob(apiJobDetails);
            assertEquals("{\"value\":\"key1\",\"value2\":\"key2\",\"value3\":\"key3\","
                + "\"value4\":{\"value1\":\"key1\","
                + "\"value2\":\"key2\"}}", apiJobDetails.getPayload(),"Payload must match");
        }

        private void validateDefaultCreateJob(APIJobDetails apiJobDetails) {
            Information information = apiJobDetails.getInformation();
            assertNotNull(information,"Information must not be null");
            assertEquals("Health Check", information.getName(),"Name must match");
            assertEquals("Checks to ensure the health of the application is okay", information.getDescription(),
                "Description must match");
            assertThat("Tags should match",information.getTags(), hasItems("Health Check"));
            assertEquals(1, information.getTags().size(),"Tag size must match");
            //Headers

            assertEquals(2, apiJobDetails.getHeaders().size(),"Header size must match");
            assertEquals("testHeaderValue", apiJobDetails.getHeaders().get("testHeader"),
                "Header value must match");
            assertEquals("testHeaderValue2", apiJobDetails.getHeaders().get("testHeader2"),
                "Header value must match");
            //Validations
            assertEquals(6, apiJobDetails.getValidations().size(),"Validation size must match");
            assertInstanceOf(StatusCodeAPIValidation.class, apiJobDetails.getValidations().get(0));
            StatusCodeAPIValidation statusCodeAPIValidation =
                (StatusCodeAPIValidation) apiJobDetails.getValidations().get(0);
            assertEquals(ValidationType.STATUS_CODE, statusCodeAPIValidation.getType(),"Type must match");
            assertEquals(201, statusCodeAPIValidation.getExpectedStatusCode(),"Status code must match");

            assertInstanceOf(MaxResponseTimeAPIValidation.class, apiJobDetails.getValidations().get(1));
            MaxResponseTimeAPIValidation maxResponseTimeAPIValidation =
                (MaxResponseTimeAPIValidation) apiJobDetails.getValidations().get(1);
            assertEquals(ValidationType.MAX_RESPONSE_TIME, maxResponseTimeAPIValidation.getType(),"Type must match");
            assertEquals(1000, maxResponseTimeAPIValidation.getMaxResponseTimeMS(),"Max response time must match");

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(2));
            JsonPathAPIValidation jsonPathAPIValidation = (JsonPathAPIValidation) apiJobDetails.getValidations().get(2);
            assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidation.getType(),"Type must match");
            assertEquals("status", jsonPathAPIValidation.getPath(),"Path must match");
            assertEquals("UP", jsonPathAPIValidation.getExpectedResponse(),"Expected response must match");

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(3));
            JsonPathAPIValidation jsonPathAPIValidationDB =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(3);
            assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationDB.getType(),"Type must match");
            assertEquals("components.db.status", jsonPathAPIValidationDB.getPath(),"Path must match");
            assertEquals("UP", jsonPathAPIValidationDB.getExpectedResponse(),"Expected response must match");

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(4));
            JsonPathAPIValidation jsonPathAPIValidationDiskSpace =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(4);
            assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationDiskSpace.getType(),"Type must match");
            assertEquals("components.diskspace.status", jsonPathAPIValidationDiskSpace.getPath(),"Path must match");
            assertEquals("UP", jsonPathAPIValidationDiskSpace.getExpectedResponse(),"Expected response must match");

            assertInstanceOf(JsonPathAPIValidation.class, apiJobDetails.getValidations().get(5));
            JsonPathAPIValidation jsonPathAPIValidationPing =
                (JsonPathAPIValidation) apiJobDetails.getValidations().get(5);
            assertEquals(ValidationType.JSON_PATH, jsonPathAPIValidationPing.getType(),"Type must match");
            assertEquals("components.ping.status", jsonPathAPIValidationPing.getPath(),"Path must match");
            assertEquals("UP", jsonPathAPIValidationPing.getExpectedResponse(),"Expected response must match");
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

            MockHttpServletRequestBuilder requestBuilder =
                get(SEARCH_API_JOB_URL).contentType(MediaType.APPLICATION_JSON);
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
                verify(jobDetailsMapper, times(status == HttpStatus.NOT_FOUND
                    ? 0
                    : 1)).toJobDetailsJobDetailsList(any());
            } else {
                verify(jobService, never()).getJobs(any());
                verify(jobDetailsMapper, never()).toJobDetailsJobDetailsList(any());
            }
        }

        protected void callAndExpectValidResponse(Map<String, String[]> queryParams) throws Exception {

            MockHttpServletRequestBuilder requestBuilder =
                get(SEARCH_API_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                requestBuilder.queryParam(entry.getKey(), entry.getValue());
            }


            List<APIJobDetailsResponse> responseList = new ArrayList<>();

            responseList.add(GenerateUtil.generateAPIJobDetailsResponse());
            responseList.add(GenerateUtil.generateAPIJobDetailsResponse());
            responseList.add(GenerateUtil.generateAPIJobDetailsResponse());

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
                assertEquals(queryParams.get("job_key")[0], jobSearchFilter.getJobKey(),"Key must match");
            } else {
                assertNull(jobSearchFilter.getJobKey(),"Job Key should be null");
            }

            if (queryParams.containsKey("tag")) {
                String[] expectedTags = queryParams.get("tag");
                Set<String> actualTags = jobSearchFilter.getTags();
                assertEquals(expectedTags.length, actualTags.size(),"Tag size must match");
                assertThat("Tags should match",actualTags, hasItems(expectedTags));
            } else {
                assertNull(jobSearchFilter.getTags(),"Tags should be null");
            }
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeNotFound() throws Exception {
            when(jobService.getJobs(any())).thenThrow(new NotFoundException("No Jobs found for the provided filter"));
            callAndExpectErrorResponse(Collections.emptyMap(), "NOT_FOUND",
                "The requested resource could not be located.",
                HttpStatus.NOT_FOUND, true);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidJobKey() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(
                Map.of("job_key", new String[]{"IN"}),
                "getJobs.jobKey: must match \\\"[A-Z_0-9]{3,50}\\\"");
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeTooLongTags() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(
                Map.of("tag", new String[]{RandomStringUtils.random(APIConstantsTest.DEFAULT_MAX_LENGTH_SHORT)}),
                "getJobs.tags[].<iterable element>: length must be between 0 and 250");
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeBlankTags() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(
                Map.of("tag", new String[]{" "}),
                "getJobs.tags[].<iterable element>: must not be blank");
        }


        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveOneTag() throws Exception {
            callAndExpectValidResponse(Map.of("tag", new String[]{"ValidTag"}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveMultipleTag() throws Exception {
            callAndExpectValidResponse(Map.of("tag", new String[]{"ValidTag", "ValidTag2", "ValidTag3", "ValidTag4"}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveJobKey() throws Exception {
            callAndExpectValidResponse(Map.of("job_key", new String[]{"ABC"}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveJobKeyWithTags() throws Exception {
            callAndExpectValidResponse(
                Map.of(
                    "job_key", new String[]{"ABC"},
                    "tag", new String[]{"ValidTag", "ValidTag2", "ValidTag3", "ValidTag4"}
                ));
        }
    }
}
