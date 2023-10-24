package uk.gov.hmcts.juror.scheduler.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.TaskSearchFilter;
import uk.gov.hmcts.juror.scheduler.datastore.repository.TaskRepository;
import uk.gov.hmcts.juror.scheduler.service.contracts.ActionService;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    private final JobService jobService;

    private final ActionService actionService;


    @Autowired
    public TaskServiceImpl(@Lazy JobService jobService, TaskRepository taskRepository,
                           @Lazy ActionService actionService) {
        this.taskRepository = taskRepository;
        this.jobService = jobService;
        this.actionService = actionService;
    }


    @Override
    public TaskEntity createTask(APIJobDetailsEntity apiJobDetailsEntity) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setJob(apiJobDetailsEntity);
        taskEntity.setStatus(Status.PENDING);
        return saveTask(taskEntity);
    }

    @Override
    public TaskEntity saveTask(TaskEntity task) {
        TaskEntity actionTaskEntity = this.actionService.taskUpdated(task);
        return taskRepository.save(actionTaskEntity);
    }

    @Override
    public TaskEntity getLatestTask(String jobKey) {
        if (!jobService.doesJobExist(jobKey)) {
            throw new NotFoundException("Job with key '" + jobKey + "' not found");
        }
        return taskRepository.findFirstByJobKeyOrderByCreatedAt(jobKey);
    }

    @Override
    public TaskEntity getLatestTask(String jobKey, long taskId) {
        if (!jobService.doesJobExist(jobKey)) {
            throw new NotFoundException("Job with key '" + jobKey + "' not found");
        }
        Optional<TaskEntity> taskEntityOptional = taskRepository.findByJobKeyAndTaskId(jobKey, taskId);
        if (taskEntityOptional.isPresent()) {
            return taskEntityOptional.get();
        }
        throw new NotFoundException("Task not found for JobKey: " + jobKey + " and taskId " + taskId);
    }

    @Override
    public void updateStatus(String jobKey, long taskId, StatusUpdate statusUpdate) {
        TaskEntity taskEntity = getLatestTask(jobKey, taskId);
        taskEntity.setStatus(statusUpdate.getStatus());
        if (statusUpdate.getMessage() != null) {
            taskEntity.setMessage(statusUpdate.getMessage());
        }
        saveTask(taskEntity);
    }

    @Override
    @Transactional
    public void deleteAllByJobKey(String jobKey) {
        taskRepository.deleteAllByJobKey(jobKey);
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public List<TaskEntity> getTasks(TaskSearchFilter searchFilter) {
        List<Specification<TaskEntity>> specifications = new ArrayList<>();

        LocalDateTime fromDate = Optional.ofNullable(searchFilter.getFromDate())
            .orElseGet(() -> LocalDateTime.now().minusDays(7));

        specifications.add(TaskRepository.Specs.byCreateDateGreaterThan(fromDate));

        if (searchFilter.getJobKey() != null) {
            specifications.add(TaskRepository.Specs.byJobKey(searchFilter.getJobKey()));
        }
        if (searchFilter.getStatuses() != null) {
            specifications.add(TaskRepository.Specs.byStatus(searchFilter.getStatuses()));
        }
        List<TaskEntity> taskEntities =
            taskRepository.findAll(TaskRepository.Specs.orderByCreatedOn(Specification.allOf(
                specifications
            )));
        if (taskEntities.isEmpty()) {
            throw new NotFoundException("No tasks found for the provided filter");
        }
        return taskEntities;
    }

    @Override
    public List<TaskEntity> getTasks(String jobKey) {
        if (!jobService.doesJobExist(jobKey)) {
            throw new NotFoundException("Job with key '" + jobKey + "' not found");
        }
        return taskRepository.findAllByJobKey(jobKey);
    }
}
