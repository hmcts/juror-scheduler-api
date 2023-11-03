package uk.gov.hmcts.juror.scheduler.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.TaskSearchFilter;
import uk.gov.hmcts.juror.scheduler.datastore.repository.TaskRepository;
import uk.gov.hmcts.juror.scheduler.service.contracts.ActionService;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        TaskServiceImpl.class
    }
)
@DisplayName("TaskServiceImpl")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports"
})
class TaskServiceImplTest {

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private JobService jobService;

    @MockBean
    private ActionService actionService;

    @Autowired
    private TaskServiceImpl taskService;

    private static final String JOB_KEY = "ABC123";

    @DisplayName("public TaskEntity createTask(APIJobDetailsEntity apiJobDetailsEntity)")
    @Nested
    class CreateTask {
        @Test
        @DisplayName("Success")
        void positiveCreateTaskSuccessfully() {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder().key(JOB_KEY).build();

            when(taskRepository.save(any(TaskEntity.class)))
                .thenAnswer(invocation -> {
                    TaskEntity task = invocation.getArgument(0);
                    task.setTaskId(5L);
                    return task;
                });
            when(actionService.taskUpdated(any(TaskEntity.class)))
                .thenAnswer(invocation -> {
                    TaskEntity task = invocation.getArgument(0);
                    task.setTaskId(5L);
                    return task;
                });

            TaskEntity taskEntity = taskService.createTask(apiJobDetailsEntity);
            assertEquals(5L, taskEntity.getTaskId(), "Task id must match");
            assertEquals(apiJobDetailsEntity, taskEntity.getJob(), "Job must match");
            assertEquals(Status.PENDING, taskEntity.getStatus(), "Status must match");

            verify(actionService, times(1)).taskUpdated(any());
            verify(taskRepository, times(1)).save(any());
        }
    }

    @DisplayName("public TaskEntity saveTask(TaskEntity task)")
    @Nested
    class SaveTask {
        @Test
        @DisplayName("Success")
        void positiveSaveTaskSuccessfully() {
            TaskEntity taskEntityProvided = new TaskEntity();
            TaskEntity savedTaskEntity = new TaskEntity();
            savedTaskEntity.setTaskId(4L);
            when(taskRepository.save(savedTaskEntity)).thenReturn(savedTaskEntity);
            when(actionService.taskUpdated(taskEntityProvided)).thenReturn(savedTaskEntity);
            assertEquals(savedTaskEntity, taskService.saveTask(taskEntityProvided), "Task must match");

            verify(actionService, times(1)).taskUpdated(taskEntityProvided);
            verify(taskRepository, times(1)).save(savedTaskEntity);
        }
    }

    @DisplayName("public List<TaskEntity> getTasks(String jobKey)")
    @Nested
    class GetTasksForJob {
        @Test
        @DisplayName("Success")
        void positiveGetTasksSuccessfully() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            List<TaskEntity> taskEntityList = new ArrayList<>();
            taskEntityList.add(TaskEntity.builder().taskId(1L).build());
            taskEntityList.add(TaskEntity.builder().taskId(2L).build());
            taskEntityList.add(TaskEntity.builder().taskId(3L).build());

            when(taskRepository.findAllByJobKey(JOB_KEY)).thenReturn(taskEntityList);

            assertEquals(taskEntityList, taskService.getTasks(JOB_KEY), "Task must match");
            verify(taskRepository, times(1)).findAllByJobKey(JOB_KEY);
        }

        @Test
        @DisplayName("Job not Found")
        void negativeJobNotFound() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(false);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getTasks(JOB_KEY));
            assertEquals("Job with key '" + JOB_KEY + "' not found",
                exception.getMessage(), "Message must match");
        }
    }

    @DisplayName(" public TaskEntity getTask(String jobKey)")
    @Nested
    class GetTaskForJob {
        @Test
        @DisplayName("Success")
        void positiveGetTasksSuccessfully() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            TaskEntity task = TaskEntity.builder().taskId(1L).build();
            when(taskRepository.findFirstByJobKeyOrderByCreatedAtDesc(JOB_KEY)).thenReturn(task);
            assertEquals(task, taskService.getLatestTask(JOB_KEY), "Task must match");
            verify(taskRepository, times(1)).findFirstByJobKeyOrderByCreatedAtDesc(JOB_KEY);
        }

        @Test
        @DisplayName("Job not Found")
        void negativeJobNotFound() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(false);
            NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.getLatestTask(JOB_KEY));
            assertEquals("Job with key '" + JOB_KEY + "' not found",
                exception.getMessage(), "Message must match");
        }
    }

    @DisplayName("public TaskEntity getTask(String jobKey, long taskId)")
    @Nested
    class GetTaskForJobKeyAndTaskId {
        @Test
        @DisplayName("Success")
        void positiveGetTaskSuccessfully() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            long taskId = 1L;
            Optional<TaskEntity> optional = Optional.of(TaskEntity.builder().taskId(taskId).build());
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);

            assertEquals(optional.get(), taskService.getLatestTask(JOB_KEY, taskId), "Task must match");
            verify(jobService, times(1)).doesJobExist(JOB_KEY);
            verify(taskRepository, times(1)).findByJobKeyAndTaskId(JOB_KEY, taskId);
        }

        @Test
        @DisplayName("Task not found")
        void negativeTaskNotFound() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            long taskId = 1L;
            Optional<TaskEntity> optional = Optional.empty();
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getLatestTask(JOB_KEY,
                taskId));
            assertEquals("Task not found for JobKey: " + JOB_KEY + " and taskId " + taskId,
                exception.getMessage(), "Message must match");

        }

        @Test
        @DisplayName("Job not Found")
        void negativeJobNotFound() {
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(false);

            NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taskService.getLatestTask(JOB_KEY, 1L));
            assertEquals("Job with key '" + JOB_KEY + "' not found",
                exception.getMessage(), "Message must match");
        }
    }


    @DisplayName("public void updateStatus(String jobKey, long taskId, StatusUpdate statusUpdate)")
    @Nested
    class UpdateStatus {

        @ParameterizedTest(name = "Success - Only status: {0}")
        @EnumSource(Status.class)
        void positiveUpdateStatusOnly(Status status) {
            long taskId = 1L;
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);
            when(taskRepository.save(taskEntity)).thenReturn(taskEntity);
            when(actionService.taskUpdated(taskEntity)).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(status);

            taskService.updateStatus(JOB_KEY, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(taskEntity);
            assertEquals(status, taskEntity.getStatus(), "Status must match");
            assertEquals("Msg", taskEntity.getMessage(), "Message must match");
        }

        @Test
        @DisplayName("Success - Status and message")
        void positiveUpdateStatusAndMessage() {
            long taskId = 1L;
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);
            when(taskRepository.save(taskEntity)).thenReturn(taskEntity);
            when(actionService.taskUpdated(taskEntity)).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(Status.VALIDATION_PASSED);
            statusUpdate.setMessage("New Message");

            taskService.updateStatus(JOB_KEY, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(taskEntity);
            assertEquals(Status.VALIDATION_PASSED, taskEntity.getStatus(), "Status must match");
            assertEquals("New Message", taskEntity.getMessage(), "Message must match");
        }

        @Test
        @DisplayName("Success - Status and meta_data")
        void positiveUpdateStatusAndMetaData() {
            long taskId = 1L;
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);
            when(taskRepository.save(taskEntity)).thenReturn(taskEntity);
            when(actionService.taskUpdated(taskEntity)).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(Status.VALIDATION_PASSED);
            statusUpdate.setMetaData(Map.of("MyKey", "MyValue"));

            taskService.updateStatus(JOB_KEY, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(taskEntity);
            assertEquals(Status.VALIDATION_PASSED, taskEntity.getStatus(), "Status must match");
            assertEquals("Msg", taskEntity.getMessage(), "Message must not be updated");

            assertEquals(1, taskEntity.getMetaData().size(), "Size must match");
            assertThat("Meta_data must match", taskEntity.getMetaData(), hasEntry("MyKey", "MyValue"));
        }

        @Test
        @DisplayName("Success - Status and meta_data. Multiple updates - Meta_data should append")
        void positiveUpdateStatusAndMetaDataMultiple() {
            long taskId = 1L;
            when(jobService.doesJobExist(JOB_KEY)).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(JOB_KEY, taskId)).thenReturn(optional);
            when(taskRepository.save(taskEntity)).thenReturn(taskEntity);
            when(actionService.taskUpdated(taskEntity)).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(Status.VALIDATION_PASSED);
            statusUpdate.setMetaData(Map.of("MyKey", "MyValue"));

            taskService.updateStatus(JOB_KEY, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(taskEntity);
            assertEquals(Status.VALIDATION_PASSED, taskEntity.getStatus(), "Status must match");
            assertEquals("Msg", taskEntity.getMessage(), "Message must not be updated");

            assertEquals(1, taskEntity.getMetaData().size(), "Size must match");
            assertThat("Meta_data must match", taskEntity.getMetaData(), hasEntry("MyKey", "MyValue"));

            statusUpdate.setMetaData(Map.of("NewKey", "NewValue"));
            taskService.updateStatus(JOB_KEY, taskId, statusUpdate);

            verify(taskRepository, times(2)).save(taskEntity);
            assertEquals(Status.VALIDATION_PASSED, taskEntity.getStatus(), "Status must match");
            assertEquals("Msg", taskEntity.getMessage(), "Message must not be updated");

            assertEquals(2, taskEntity.getMetaData().size(), "Size must match");
            assertThat("Meta_data must match", taskEntity.getMetaData(), hasEntry("MyKey", "MyValue"));
            assertThat("Meta_data must match", taskEntity.getMetaData(), hasEntry("NewKey", "NewValue"));
        }
    }


    @DisplayName("public void deleteAllByJobKey(String jobKey)")
    @Nested
    class DeleteALlByJobKey {
        @Test
        @DisplayName("Success")
        void positiveDeleteAllByJobKey() {
            taskService.deleteAllByJobKey(JOB_KEY);
            verify(taskRepository, times(1)).deleteAllByJobKey(JOB_KEY);
        }
    }

    @DisplayName("public List<TaskEntity> getTasks(TaskSearchFilter searchFilter)")
    @Nested
    class GetTasksBySearchFilter {

        @Captor
        private ArgumentCaptor<List<Specification<TaskEntity>>> captor;

        @Test


        @DisplayName("No search Filters")
        void positiveNoSearchFilters() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Size must match");
                        assertThat("Returned jobs should match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs must not be null");
                        assertEquals(1, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(7)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Job Key Filters")
        void positiveJobKeyFilter() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().jobKey(JOB_KEY).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Returned job size must match");
                        assertThat("Returned jobs should match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs should not be null");
                        assertEquals(2, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(7)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(JOB_KEY), times(1));
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Single Status Filters")
        void positiveSingleStatusFilter() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                Set<Status> statusSet = new HashSet<>();
                statusSet.add(Status.PENDING);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().statuses(statusSet).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Returned Job size must match");
                        assertThat("Returned jobs should match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs must not be null");
                        assertEquals(2, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(7)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(statusSet), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple Status Filters")
        void positiveMultipleStatusFilter() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                Set<Status> statusSet = new HashSet<>();
                statusSet.add(Status.PENDING);
                statusSet.add(Status.VALIDATION_PASSED);
                statusSet.add(Status.VALIDATION_FAILED);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().statuses(statusSet).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Returned Job size must match");
                        assertThat("Returned jobs must match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs must not be null");
                        assertEquals(2, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(7)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(statusSet), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }

        }

        @Test
        @DisplayName("From date Filters")
        void positiveFromDateFilter() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder()
                    .fromDate(currentLocalDateTime.minusDays(3)).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Returned Job Size must match");
                        assertThat("Returned jobs must match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs must not be null");
                        assertEquals(1, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(3)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple filters")
        void positiveMultipleFilters() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                Set<Status> statusSet = new HashSet<>();
                statusSet.add(Status.PENDING);
                statusSet.add(Status.VALIDATION_PASSED);
                statusSet.add(Status.VALIDATION_PASSED);
                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder()
                    .jobKey(JOB_KEY)
                    .statuses(statusSet)
                    .fromDate(currentLocalDateTime.minusDays(3)).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                         Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                             Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);
                        assertEquals(tasks.size(), returnedJobs.size(), "Returned Jobs must match");
                        assertThat("Returned jobs must match", returnedJobs,
                            hasItems(tasks.toArray(new TaskEntity[0])));

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs, "Specs must not be null");
                        assertEquals(3, specs.size(), "Spec size must match");
                    }
                }
                utilities.verify(
                    () -> TaskRepository.Specs.byCreateDateGreaterThan(currentLocalDateTime.minusDays(3)),
                    times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(JOB_KEY), times(1));
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("No Tasks found")
        void negativeNotTasksFound() {

            when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(
                Collections.emptyList());
            TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().build();
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                taskService.getTasks(taskSearchFilter);
            });

            assertEquals("No tasks found for the provided filter",
                exception.getMessage(), "Message must match");
        }
    }
}
