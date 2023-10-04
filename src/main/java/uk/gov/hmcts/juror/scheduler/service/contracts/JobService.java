package uk.gov.hmcts.juror.scheduler.service.contracts;

import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;

import java.util.List;

public interface JobService {
    void deleteJob(String jobKey);
    void disable(String jobKey);
    void enable(String jobKey);
    void executeJob(String jobKey);

    boolean doesJobExist(String jobKey);
    APIJobDetailsEntity getJob(String key);

    APIJobDetailsEntity updateJob(String jobKey, APIJobPatch jobPatch);
    void createJob(APIJobDetails jobDetails);
    APIJobDetailsEntity save(APIJobDetailsEntity jobDetailsEntity);

    List<APIJobDetailsEntity> getJobs(JobSearchFilter filter);
}
