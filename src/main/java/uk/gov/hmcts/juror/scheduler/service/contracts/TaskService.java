package uk.gov.hmcts.juror.scheduler.service.contracts;

import uk.gov.hmcts.juror.scheduler.api.model.job.details.StatusUpdate;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.TaskSearchFilter;

import java.util.List;

public interface TaskService {
    TaskEntity createTask(APIJobDetailsEntity apiJobDetailsEntity);

    TaskEntity saveTask(TaskEntity task);

    List<TaskEntity> getTasks(String jobKey);

    List<TaskEntity> getTasks(TaskSearchFilter searchFilter);

    TaskEntity getLatestTask(String jobKey);

    TaskEntity getLatestTask(String jobKey, long taskId);

    void updateStatus(String jobKey, long taskId, StatusUpdate statusUpdate);


    void logTaskEntity(TaskEntity task);

    void deleteAllByJobKey(String jobKey);
}
