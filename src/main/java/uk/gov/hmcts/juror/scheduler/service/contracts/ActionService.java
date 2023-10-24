package uk.gov.hmcts.juror.scheduler.service.contracts;

import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;

public interface ActionService {
    TaskEntity taskUpdated(TaskEntity taskEntity);
}
