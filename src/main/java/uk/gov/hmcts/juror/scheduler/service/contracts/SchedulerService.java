package uk.gov.hmcts.juror.scheduler.service.contracts;

import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;

public interface SchedulerService {
    void register(APIJobDetailsEntity jobDetails);
    void unregister(String jobKey);

    void disable(String jobKey);
    void enable(String jobKey);
    void executeJob(String jobKey);
    boolean isScheduled(String jobKey);

    boolean isEnabled(String jobKey);

    boolean isDisabled(String jobKey);
}
