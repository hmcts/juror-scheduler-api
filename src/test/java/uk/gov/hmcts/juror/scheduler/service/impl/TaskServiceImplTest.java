package uk.gov.hmcts.juror.scheduler.service.impl;

import org.junit.jupiter.api.Assertions;
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
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.testSupport.TestSpecification;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class TaskServiceImplTest {

    @MockBean
    private TaskRepository taskRepository;
    @MockBean
    private JobService jobService;

    @Autowired
    private TaskServiceImpl taskService;

    private final static String jobKey = "ABC123";

    @DisplayName("public TaskEntity createTask(APIJobDetailsEntity apiJobDetailsEntity)")
    @Nested
    class CreateTask {
        @Test
        @DisplayName("Success")
        void positive_create_task_successfully() {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder().key(jobKey).build();

            when(taskRepository.save(any(TaskEntity.class)))
                    .thenAnswer(invocation -> {
                        TaskEntity task = invocation.getArgument(0);
                        task.setTaskId(5L);
                        return task;
                    });
            TaskEntity taskEntity = taskService.createTask(apiJobDetailsEntity);
            assertEquals(5L, taskEntity.getTaskId());
            Assertions.assertEquals(apiJobDetailsEntity, taskEntity.getJob());
            Assertions.assertEquals(Status.PENDING, taskEntity.getStatus());
        }
    }

    @DisplayName("public TaskEntity saveTask(TaskEntity task)")
    @Nested
    class SaveTask {
        @Test
        @DisplayName("Success")
        void positive_save_task_successfully() {
            TaskEntity taskEntityProvided = new TaskEntity();
            TaskEntity savedTaskEntity = new TaskEntity();
            savedTaskEntity.setTaskId(4L);
            when(taskRepository.save(eq(taskEntityProvided))).thenReturn(savedTaskEntity);
            Assertions.assertEquals(savedTaskEntity, taskService.saveTask(taskEntityProvided));
        }
    }

    @DisplayName("public List<TaskEntity> getTasks(String jobKey)")
    @Nested
    class GetTasksForJob {
        @Test
        @DisplayName("Success")
        void positive_get_tasks_successfully() {
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(true);
            List<TaskEntity> taskEntityList = new ArrayList<>();
            taskEntityList.add(TaskEntity.builder().taskId(1L).build());
            taskEntityList.add(TaskEntity.builder().taskId(2L).build());
            taskEntityList.add(TaskEntity.builder().taskId(3L).build());

            when(taskRepository.findAllByJobKey(eq(jobKey))).thenReturn(taskEntityList);

            assertEquals(taskEntityList, taskService.getTasks(jobKey));
            verify(taskRepository, times(1)).findAllByJobKey(eq(jobKey));
        }

        @Test
        @DisplayName("Job not Found")
        void negative_job_not_found() {
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(false);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getTasks(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", exception.getMessage());
        }
    }

    @DisplayName(" public TaskEntity getTask(String jobKey)")
    @Nested
    class GetTaskForJob {
        @Test
        @DisplayName("Success")
        void positive_get_tasks_successfully() {
            when(jobService.doesJobExist(jobKey)).thenReturn(true);
            TaskEntity task = TaskEntity.builder().taskId(1L).build();
            when(taskRepository.findFirstByJobKeyOrderByCreatedAt(jobKey)).thenReturn(task);
            Assertions.assertEquals(task, taskService.getLatestTask(jobKey));
            verify(taskRepository, times(1)).findFirstByJobKeyOrderByCreatedAt(jobKey);
        }

        @Test
        @DisplayName("Job not Found")
        void negative_job_not_found() {
            when(jobService.doesJobExist(jobKey)).thenReturn(false);
            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getLatestTask(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", exception.getMessage());
        }
    }

    @DisplayName("public TaskEntity getTask(String jobKey, long taskId)")
    @Nested
    class GetTaskForJobKeyAndTaskId {
        @Test
        @DisplayName("Success")
        void positive_get_task_successfully() {
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(true);
            long taskId = 1L;
            Optional<TaskEntity> optional = Optional.of(TaskEntity.builder().taskId(taskId).build());
            when(taskRepository.findByJobKeyAndTaskId(eq(jobKey), eq(taskId))).thenReturn(optional);

            Assertions.assertEquals(optional.get(), taskService.getLatestTask(jobKey, taskId));
            verify(jobService, times(1)).doesJobExist(eq(jobKey));
            verify(taskRepository, times(1)).findByJobKeyAndTaskId(eq(jobKey), eq(taskId));
        }

        @Test
        @DisplayName("Task not found")
        void negative_task_not_found() {
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(true);
            long taskId = 1L;
            Optional<TaskEntity> optional = Optional.empty();
            when(taskRepository.findByJobKeyAndTaskId(eq(jobKey), eq(taskId))).thenReturn(optional);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getLatestTask(jobKey,
                    taskId));
            assertEquals("Task not found for JobKey: " + jobKey + " and taskId " + taskId, exception.getMessage());

        }

        @Test
        @DisplayName("Job not Found")
        void negative_job_not_found() {
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(false);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> taskService.getLatestTask(jobKey,
                    1L));
            assertEquals("Job with key '" + jobKey + "' not found", exception.getMessage());
        }
    }


    @DisplayName("public void updateStatus(String jobKey, long taskId, StatusUpdate statusUpdate)")
    @Nested
    class UpdateStatus {

        @ParameterizedTest(name = "Success - Only status: {0}")
        @EnumSource(Status.class)
        void positive_update_status_only(Status status) {
            long taskId = 1L;
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(eq(jobKey), eq(taskId))).thenReturn(optional);
            when(taskRepository.save(eq(taskEntity))).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(status);

            taskService.updateStatus(jobKey, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(eq(taskEntity));
            Assertions.assertEquals(status, taskEntity.getStatus());
            assertEquals("Msg", taskEntity.getMessage());
        }

        @Test
        @DisplayName("Success - Status and message")
        void positive_update_status_and_message() {
            long taskId = 1L;
            when(jobService.doesJobExist(eq(jobKey))).thenReturn(true);
            TaskEntity taskEntity = TaskEntity.builder().taskId(taskId).message("Msg").build();
            Optional<TaskEntity> optional = Optional.of(taskEntity);
            when(taskRepository.findByJobKeyAndTaskId(eq(jobKey), eq(taskId))).thenReturn(optional);
            when(taskRepository.save(eq(taskEntity))).thenReturn(taskEntity);

            StatusUpdate statusUpdate = new StatusUpdate();
            statusUpdate.setStatus(Status.VALIDATION_PASSED);
            statusUpdate.setMessage("New Message");

            taskService.updateStatus(jobKey, taskId, statusUpdate);

            verify(taskRepository, times(1)).save(eq(taskEntity));
            Assertions.assertEquals(Status.VALIDATION_PASSED, taskEntity.getStatus());
            assertEquals("New Message", taskEntity.getMessage());
        }
    }


    @DisplayName("public void deleteAllByJobKey(String jobKey)")
    @Nested
    class DeleteALlByJobKey {
        @Test
        @DisplayName("Success")
        void positive_delete_all_by_job_key() {
            taskService.deleteAllByJobKey(jobKey);
            verify(taskRepository, times(1)).deleteAllByJobKey(eq(jobKey));
        }
    }

    @DisplayName("public List<TaskEntity> getTasks(TaskSearchFilter searchFilter)")
    @Nested
    class GetTasksBySearchFilter {

        @Captor
        private ArgumentCaptor<List<Specification<TaskEntity>>> captor;

        @Test


        @DisplayName("No search Filters")
        void positive_no_search_filters() {
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

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(1, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(7))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Job Key Filters")
        void positive_job_key_filter() {
            LocalDateTime currentLocalDateTime = LocalDateTime.now();
            try (MockedStatic<TaskRepository.Specs> utilities = Mockito.mockStatic(TaskRepository.Specs.class)) {
                List<TaskEntity> tasks = new ArrayList<>();
                tasks.add(TaskEntity.builder().taskId(1L).build());
                tasks.add(TaskEntity.builder().taskId(2L).build());
                tasks.add(TaskEntity.builder().taskId(3L).build());
                when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(tasks);

                TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().jobKey(jobKey).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                             Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                                 Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(2, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(7))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(eq(jobKey)), times(1));
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Single Status Filters")
        void positive_single_status_filter() {
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

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(2, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(7))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(eq(statusSet)), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple Status Filters")
        void positive_multiple_status_filter() {
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

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(2, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(7))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(eq(statusSet)), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }

        }

        @Test
        @DisplayName("From date Filters")
        void positive_from_date_filter() {
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

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(1, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(3))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), never());
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple filters")
        void positive_multiple_filters() {
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
                        .jobKey(jobKey)
                        .statuses(statusSet)
                        .fromDate(currentLocalDateTime.minusDays(3)).build();

                try (MockedStatic<Specification> specificationMockedStatic =
                             Mockito.mockStatic(Specification.class)) {

                    try (MockedStatic<LocalDateTime> localDateTimeMock =
                                 Mockito.mockStatic(LocalDateTime.class)) {
                        localDateTimeMock.when(LocalDateTime::now).thenReturn(currentLocalDateTime);

                        List<TaskEntity> returnedJobs = taskService.getTasks(taskSearchFilter);

                        specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                        List<Specification<TaskEntity>> specs = captor.getValue();
                        assertNotNull(specs);
                        assertEquals(3, specs.size());
                        assertEquals(tasks.size(), returnedJobs.size());
                        assertThat(returnedJobs, hasItems(tasks.toArray(new TaskEntity[0])));
                    }
                }
                utilities.verify(
                        () -> TaskRepository.Specs.byCreateDateGreaterThan(eq(currentLocalDateTime.minusDays(3))),
                        times(1));

                utilities.verify(() -> TaskRepository.Specs.byJobKey(eq(jobKey)), times(1));
                utilities.verify(() -> TaskRepository.Specs.byStatus(any()), times(1));
                utilities.verify(() -> TaskRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("No Tasks found")
        void negative_not_tasks_found() {

            when(taskRepository.findAll(ArgumentMatchers.<Specification<TaskEntity>>any())).thenReturn(
                    Collections.emptyList());
            TaskSearchFilter taskSearchFilter = TaskSearchFilter.builder().build();
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                taskService.getTasks(taskSearchFilter);
            });

            assertEquals("No tasks found for the provided filter", exception.getMessage());
        }

        private void setupSpecificationMocks(MockedStatic<TaskRepository.Specs> utilities) {
            utilities.when(() -> TaskRepository.Specs.byJobKey(any()))
                    .thenReturn(new TestSpecification("byJobKey"));
            utilities.when(() -> TaskRepository.Specs.byStatus(any()))
                    .thenReturn(new TestSpecification("byStatus"));
            utilities.when(() -> TaskRepository.Specs.byCreateDateGreaterThan(any()))
                    .thenReturn(new TestSpecification("byCreateDateGreaterThan"));
            utilities.when(() -> TaskRepository.Specs.orderByCreatedOn(any()))
                    .thenReturn(new TestSpecification("orderByCreatedOn"));

        }
    }
}
