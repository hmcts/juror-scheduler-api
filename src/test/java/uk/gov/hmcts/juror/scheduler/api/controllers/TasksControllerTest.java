package uk.gov.hmcts.juror.scheduler.api.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.TaskSearchFilter;
import uk.gov.hmcts.juror.scheduler.mapping.TaskMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.testsupport.ControllerTestSupport;
import uk.gov.hmcts.juror.scheduler.testsupport.TestUtil;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TasksController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
    classes = {
        TasksController.class,
        ExceptionHandling.class
    }
)
@DisplayName("Controller: /tasks")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter",
    "PMD.TooManyMethods"})
class TasksControllerTest {
    private static final String CONTROLLER_BASEURL = "/tasks";
    private static final String SEARCH_TASK_JOB_URL = CONTROLLER_BASEURL + "/search";


    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskMapper taskMapper;

    @Nested
    @DisplayName("GET " + SEARCH_TASK_JOB_URL)
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
                                                  boolean taskServiceCalled) throws Exception {

            MockHttpServletRequestBuilder requestBuilder =
                get(SEARCH_TASK_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                requestBuilder.queryParam(entry.getKey(), entry.getValue());
            }

            this.mockMvc
                .perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(status.value()))
                .andExpect(content().json(createErrorResponseString(expectedErrorCode, expectedErrorMessage), true))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            if (taskServiceCalled) {
                verify(taskService, times(1)).getTasks(any(TaskSearchFilter.class));
                verify(taskMapper, times(status == HttpStatus.NOT_FOUND
                    ? 0
                    : 1)).toTaskList(any());
            } else {
                verify(taskService, never()).getTasks(any(TaskSearchFilter.class));
                verify(taskMapper, never()).toTaskList(any());
            }
        }

        protected void callAndExpectValidResponse(Map<String, String[]> queryParams) throws Exception {

            MockHttpServletRequestBuilder requestBuilder =
                get(SEARCH_TASK_JOB_URL).contentType(MediaType.APPLICATION_JSON);
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                requestBuilder.queryParam(entry.getKey(), entry.getValue());
            }


            List<TaskDetail> responseList = new ArrayList<>();

            responseList.add(TestUtil.generateTask());
            responseList.add(TestUtil.generateTask());
            responseList.add(TestUtil.generateTask());


            when(taskMapper.toTaskList(any())).thenReturn(responseList);

            this.mockMvc
                .perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().json(createResponseStringFromObject(responseList),
                    true));

            verify(taskMapper, times(1)).toTaskList(any());

            final ArgumentCaptor<TaskSearchFilter> captor = ArgumentCaptor.forClass(TaskSearchFilter.class);
            verify(taskService, times(1)).getTasks(captor.capture());

            final TaskSearchFilter taskSearchFilter = captor.getValue();

            if (queryParams.containsKey("job_key")) {
                assertEquals(queryParams.get("job_key")[0], taskSearchFilter.getJobKey(), "Job kye must match");
            } else {
                assertNull(taskSearchFilter.getJobKey(),"Job key must be null");
            }

            if (queryParams.containsKey("status")) {
                String[] expectedStatues = queryParams.get("status");
                Set<Status> expectedStatuesEnum =
                    Arrays.stream(expectedStatues).map(Status::valueOf).collect(Collectors.toSet());
                Set<Status> actualStatues = taskSearchFilter.getStatuses();

                assertEquals(expectedStatuesEnum.size(), actualStatues.size(), "Status size must match");
                assertThat("Statuses must match",expectedStatuesEnum, hasItems(actualStatues.toArray(new Status[0])));
            } else {
                assertNull(taskSearchFilter.getStatuses(),"Statuses must be null");
            }

            if (queryParams.containsKey("from_date")) {
                assertEquals(LocalDateTime.parse(queryParams.get("from_date")[0]), taskSearchFilter.getFromDate(),
                    "From Date must match");
            } else {
                assertNull(taskSearchFilter.getFromDate(),"From Date must be null");
            }
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeNotFound() throws Exception {
            when(taskService.getTasks(any(TaskSearchFilter.class))).thenThrow(
                new NotFoundException("No Tasks found for this search filter"));
            callAndExpectErrorResponse(Collections.emptyMap(), "NOT_FOUND",
                "The requested resource could not be located.",
                HttpStatus.NOT_FOUND, true);
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidJobKey() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(Map.of("job_key", new String[]{"IN"}),
                "getTasks.jobKey: must match \\\"[A-Z_0-9]{3,50}\\\"");
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidFromDate() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(Map.of("from_date", new String[]{"INVALID"}),
                "from_date: could not be parsed");
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void negativeInvalidStatus() throws Exception {
            callAndExpectInvalidPayloadErrorResponse(Map.of("status", new String[]{"INVALID"}),
                "status: could not be parsed");
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
        void positiveFromDate() throws Exception {
            callAndExpectValidResponse(Map.of("from_date", new String[]{LocalDateTime.now().toString()}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveStatusSingle() throws Exception {
            callAndExpectValidResponse(Map.of("status", new String[]{Status.PENDING.name()}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveStatusMultiple() throws Exception {
            callAndExpectValidResponse(Map.of("status", TestUtil.getEnumNames(Status.class)));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveStatusAndFromDate() throws Exception {
            callAndExpectValidResponse(
                Map.of("status", new String[]{Status.VALIDATION_PASSED.name()},
                    "from_date", new String[]{LocalDateTime.now().toString()}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveStatusAndJobKey() throws Exception {
            callAndExpectValidResponse(
                Map.of("status", new String[]{Status.PENDING.name()},
                    "job_key", new String[]{"ABC"}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveFromDateAndJobKey() throws Exception {
            callAndExpectValidResponse(
                Map.of("from_date", new String[]{LocalDateTime.now().toString()},
                    "job_key", new String[]{"ABC"}));
        }

        @Test
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveAllFilters() throws Exception {
            callAndExpectValidResponse(
                Map.of("status", TestUtil.getEnumNames(Status.class),
                    "from_date", new String[]{LocalDateTime.now().toString()},
                    "job_key", new String[]{"ABC"}));
        }
    }
}
