package uk.gov.hmcts.juror.scheduler.api.controllers;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
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
import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.testsupport.APIConstantsTest;
import uk.gov.hmcts.juror.scheduler.testsupport.ControllerTestSupport;
import uk.gov.hmcts.juror.scheduler.testsupport.util.GenerateUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.TestUtil;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = JobTaskController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
    classes = {
        JobTaskController.class,
        ExceptionHandling.class
    }
)
@DisplayName("Controller: /job/{job-key}/task/{task-id}")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports"
})
class JobTaskControllerTest {
    private static final String CONTROLLER_BASEURL = "/job/{job-key}/task/{task-id}";
    private static final String GET_TASK_DETAILS = CONTROLLER_BASEURL;
    private static final String UPDATE_TASK_STATUS = CONTROLLER_BASEURL + "/status";

    private static final String RESOURCE_PREFIX = "/testData/jobTaskController";


    @MockBean
    private TaskService taskService;
    @MockBean
    private TaskMapper taskMapper;

    @Nested
    @DisplayName("GET " + GET_TASK_DETAILS)
    class GetTaskDetails extends ControllerTestSupport {
        protected void callAndExpectErrorResponse(String jobKey,
                                                  Long taskId,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            this.mockMvc
                .perform(get(GET_TASK_DETAILS, jobKey, taskId).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(status.value()))
                .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(taskService, times(1)).getLatestTask(jobKey, taskId);
                verify(taskMapper, times(status == HttpStatus.NOT_FOUND
                    ? 0
                    : 1)).toTask(any());
            } else {
                verify(taskService, never()).getLatestTask(any(String.class), any(Long.class));
                verify(taskMapper, never()).toTask(any());
            }
        }

        @Test
        void positiveGetTask() throws Exception {
            final String jobKey = "ABC";
            final long taskId = 1L;
            TaskDetail taskDetail = GenerateUtil.generateTask();
            when(taskMapper.toTask(any())).thenReturn(taskDetail);

            this.mockMvc
                .perform(get(GET_TASK_DETAILS, jobKey, taskId).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(createResponseStringFromObject(taskDetail), true));
            verify(taskService, times(1)).getLatestTask(jobKey, taskId);
            verify(taskMapper, times(1)).toTask(any());
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidJobKey() throws Exception {
            callAndExpectErrorResponse("A", 1L, "INVALID_PAYLOAD",
                "getTaskDetail.jobKey: must match \\\"[A-Z_0-9]{3,50}\\\"",
                HttpStatus.BAD_REQUEST, false);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidTaskKey() throws Exception {
            callAndExpectErrorResponse("ABC", 0L, "INVALID_PAYLOAD",
                "getTaskDetail.taskId: must be greater than or equal to 1",
                HttpStatus.BAD_REQUEST, false);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeNotFound() throws Exception {
            final String jobKey = "ABC";
            final long taskId = 1L;
            doThrow(new NotFoundException("Task not found for JobKey: " + jobKey + " and taskId " + taskId))
                .when(taskService).getLatestTask(jobKey, taskId);
            callAndExpectErrorResponse(jobKey, taskId, "NOT_FOUND",
                "The requested resource could not be located.",
                HttpStatus.NOT_FOUND, true);
        }
    }

    @Nested
    @DisplayName("PUT " + UPDATE_TASK_STATUS)
    class UpdateTaskStatus extends ControllerTestSupport {


        public static Stream<Arguments> invalidUpdateJobStatusPayloadArgumentSource() {
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);

            return Stream.of(
                Arguments.arguments(null, "Unable to read payload content"),
                //Status
                Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.status", "INVALID"),
                    "Invalid status entered. Allowed values are: " + Arrays.toString(Status.values())),
                Arguments.arguments(TestUtil.deleteJsonPath(payload, "$.status"), "status: must not be null"),
                //Message

                Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.message", ""),
                    "message: length must be between 1 and 2500"),

                Arguments.arguments(TestUtil.replaceJsonPath(payload, "$.message",
                        RandomStringUtils.randomAlphabetic(APIConstantsTest.DEFAULT_MAX_LENGTH_LONG)),
                    "message: length must be between 1 and 2500")
            );
        }

        @ParameterizedTest(name = "Expect error message: {1}")
        @MethodSource("invalidUpdateJobStatusPayloadArgumentSource")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeUpdateJobStatusInvalidPayload(String payload, String expectedErrorMessage) throws Exception {
            callAndExpectErrorResponse("ABC", 1L, payload, "INVALID_PAYLOAD", expectedErrorMessage,
                HttpStatus.BAD_REQUEST, false);
        }

        protected void callAndExpectErrorResponse(String jobKey,
                                                  Long taskId,
                                                  String payload,
                                                  String expectedErrorCode,
                                                  String expectedErrorMessage,
                                                  HttpStatus status,
                                                  boolean jobServiceCalled) throws Exception {

            MockHttpServletRequestBuilder builder = put(UPDATE_TASK_STATUS, jobKey, taskId)
                .contentType(MediaType.APPLICATION_JSON);
            if (payload != null) {
                builder
                    .content(payload);
            }
            this.mockMvc
                .perform(builder)
                .andDo(print())
                .andExpect(status().is(status.value()))
                .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (jobServiceCalled) {
                verify(taskService, times(1)).updateStatus(eq(jobKey), eq(taskId), any());
            } else {
                verify(taskService, never()).updateStatus(any(String.class), any(Long.class), any());
            }
        }


        private void executePositive(String jobKey, Long taskId, String payload) throws Exception {
            this.mockMvc
                .perform(put(UPDATE_TASK_STATUS, jobKey, taskId)
                    .content(payload)
                    .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(content().string(Matchers.emptyOrNullString()));
        }

        @Test
        void positiveTypicalUpdate() throws Exception {
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);
            final String jobKey = "ABC";
            final Long taskId = 1L;

            executePositive(jobKey, taskId, payload);
            final ArgumentCaptor<StatusUpdate> captor = ArgumentCaptor.forClass(StatusUpdate.class);
            verify(taskService, times(1)).updateStatus(eq(jobKey), eq(taskId), captor.capture());
            final StatusUpdate statusUpdate = captor.getValue();

            assertEquals(Status.VALIDATION_PASSED, statusUpdate.getStatus(), "Status must match");
            assertEquals("This has passed successfully because of ayz", statusUpdate.getMessage(),
                "Message must match");
        }

        @Test
        void positiveOnlyStatusUpdate() throws Exception {
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);
            payload = TestUtil.deleteJsonPath(payload, "$.message");
            final String jobKey = "ABC";
            final Long taskId = 1L;

            executePositive(jobKey, taskId, payload);
            final ArgumentCaptor<StatusUpdate> captor = ArgumentCaptor.forClass(StatusUpdate.class);
            verify(taskService, times(1)).updateStatus(eq(jobKey), eq(taskId), captor.capture());
            final StatusUpdate statusUpdate = captor.getValue();

            assertEquals(Status.VALIDATION_PASSED, statusUpdate.getStatus(), "Status must match");
            assertNull(statusUpdate.getMessage(), "Message must be null");
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidJobKey() throws Exception {
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);
            callAndExpectErrorResponse("A", 1L, payload, "INVALID_PAYLOAD",
                "updateTaskStatus.jobKey: must match \\\"[A-Z_0-9]{3,50}\\\"",
                HttpStatus.BAD_REQUEST, false);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidTaskKey() throws Exception {
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);
            callAndExpectErrorResponse("ABC", -5L, payload, "INVALID_PAYLOAD",
                "updateTaskStatus.taskId: must be greater than or equal to 1",
                HttpStatus.BAD_REQUEST, false);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeNotFound() throws Exception {
            final String jobKey = "ABC";
            final Long taskId = 1L;
            String payload = TestUtil.readResource("updateTaskStatus.json", RESOURCE_PREFIX);
            doThrow(new NotFoundException("Task not found for JobKey: " + jobKey + " and taskId " + taskId))
                .when(taskService).updateStatus(eq(jobKey), eq(taskId), any());

            callAndExpectErrorResponse(jobKey, taskId, payload, "NOT_FOUND",
                "The requested resource could not be located.",
                HttpStatus.NOT_FOUND, true);
        }

    }
}
