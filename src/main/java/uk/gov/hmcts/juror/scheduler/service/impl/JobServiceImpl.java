package uk.gov.hmcts.juror.scheduler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.juror.scheduler.datastore.repository.JobRepository;
import uk.gov.hmcts.juror.scheduler.service.contracts.SchedulerService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.standard.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.BusinessRuleValidationException;
import uk.gov.hmcts.juror.standard.service.exceptions.GenericErrorHandlerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

    private final SchedulerService schedulerService;
    private final JobRepository jobRepository;
    private final JobDetailsMapper jobDetailsMapper;
    private final TaskService taskService;

    @Autowired
    public JobServiceImpl(SchedulerService schedulerService,
                          JobRepository jobRepository,
                          JobDetailsMapper jobDetailsMapper,
                          TaskService taskService) {
        this.schedulerService = schedulerService;
        this.jobRepository = jobRepository;
        this.jobDetailsMapper = jobDetailsMapper;
        this.taskService = taskService;
    }

    @Override
    @Transactional
    public void deleteJob(String jobKey) {
        throwErrorIfJobDoesNotExist(jobKey);
        schedulerService.unregister(jobKey);
        taskService.deleteAllByJobKey(jobKey);
        jobRepository.deleteById(jobKey);
    }

    private void throwErrorIfJobDoesNotExist(String jobKey) throws NotFoundException {
        if (!doesJobExist(jobKey)) {
            throw new NotFoundException("Job with key '" + jobKey + "' not found");
        }
    }

    @Override
    public boolean doesJobExist(String jobKey) {
        return jobRepository.existsById(jobKey);
    }

    @Override
    public void disable(String jobKey) {
        throwErrorIfJobDoesNotExist(jobKey);
        if (!schedulerService.isScheduled(jobKey)) {
            throw new BusinessRuleValidationException(new NotAScheduledJobError());
        }
        if (schedulerService.isDisabled(jobKey)) {
            throw new BusinessRuleValidationException(new JobAlreadyDisabledError());
        }
        schedulerService.disable(jobKey);
    }

    @Override
    public void enable(String jobKey) {
        throwErrorIfJobDoesNotExist(jobKey);
        if (!schedulerService.isScheduled(jobKey)) {
            throw new BusinessRuleValidationException(new NotAScheduledJobError());
        }
        if (schedulerService.isEnabled(jobKey)) {
            throw new BusinessRuleValidationException(new JobAlreadyEnabledError());
        }

        schedulerService.enable(jobKey);
    }

    @Override
    public void executeJob(String jobKey) {
        throwErrorIfJobDoesNotExist(jobKey);
        schedulerService.executeJob(jobKey);
    }

    @Override
    public APIJobDetailsEntity getJob(String key) {
        Optional<APIJobDetailsEntity> jobDetailsEntity = this.jobRepository.findById(key);
        if (jobDetailsEntity.isPresent()) {
            return jobDetailsEntity.get();
        }
        throw new NotFoundException("Job not found for key: " + key);
    }


    @Override
    @Transactional
    public void createJob(APIJobDetails jobDetails) {
        if (doesJobExist(jobDetails.getKey())) {
            throw new GenericErrorHandlerException(APIHandleableException.Type.INFORMATIONAL,
                    new KeyAlreadyInUseError(), HttpStatus.CONFLICT);
        }

        APIJobDetailsEntity jobDetailsEntity = jobDetailsMapper.toAPIJobDetailsEntity(jobDetails);
        jobDetailsMapper.updateValidationsJobs(jobDetailsEntity);
        jobDetailsEntity = this.save(jobDetailsEntity);
        if (jobDetailsEntity.getCronExpression() != null) {
            schedulerService.register(jobDetailsEntity);
        }
    }

    @Override
    public APIJobDetailsEntity save(APIJobDetailsEntity jobDetailsEntity) {
        log.debug("Saving Job: " + jobDetailsEntity.getKey());
        return jobRepository.save(jobDetailsEntity);
    }

    @Override
    public List<APIJobDetailsEntity> getJobs(JobSearchFilter searchFilter) {
        List<Specification<APIJobDetailsEntity>> specifications = new ArrayList<>();


        if (searchFilter.getJobKey() != null) {
            specifications.add(JobRepository.Specs.byJobKey(searchFilter.getJobKey()));
        }
        if (searchFilter.getTags() != null) {
            specifications.add(JobRepository.Specs.byTags(searchFilter.getTags()));
        }
        List<APIJobDetailsEntity> foundJobs = jobRepository.findAll(JobRepository.Specs.orderByCreatedOn(Specification.allOf(
                specifications
        )));

        if (foundJobs.isEmpty()) {
            throw new NotFoundException("No Jobs found for the provided filter");
        }
        return foundJobs;
    }

    @Override
    @Transactional
    public APIJobDetailsEntity updateJob(String jobKey, APIJobPatch jobPatch) {
        APIJobDetailsEntity jobDetailsEntity = getJob(jobKey);
        boolean requiresReschedule = false;

        Information information = jobPatch.getInformation();
        if (information != null) {
            if (information.getName() != null) {
                jobDetailsEntity.setName(information.getName());
            }
            if (information.getDescription() != null) {
                jobDetailsEntity.setDescription(information.getDescription());
            }
            if (information.getTags() != null) {
                jobDetailsEntity.setTags(information.getTags());
            }
        }
        if (jobPatch.getCronExpression() != null) {
            jobDetailsEntity.setCronExpression(jobPatch.getCronExpression());
            requiresReschedule = true;
        }
        if (jobPatch.getMethod() != null) {
            jobDetailsEntity.setMethod(jobPatch.getMethod());
        }
        if (jobPatch.getUrl() != null) {
            jobDetailsEntity.setUrl(jobPatch.getUrl());
        }
        if (jobPatch.getHeaders() != null) {
            jobDetailsEntity.setHeaders(jobPatch.getHeaders());
        }
        if (jobPatch.getAuthenticationDefault() != null) {
            jobDetailsEntity.setAuthenticationDefault(jobPatch.getAuthenticationDefault());
        }
        if (jobPatch.getPayload() != null) {
            jobDetailsEntity.setPayload(jobPatch.getPayload());
        }
        if (jobPatch.getValidations() != null) {
            jobDetailsEntity.setValidations(jobDetailsMapper.apiValidationEntityList(jobPatch.getValidations()));
        }
        jobDetailsEntity = save(jobDetailsEntity);
        if (requiresReschedule) {
            //TODO improve
            schedulerService.unregister(jobKey);
            schedulerService.register(jobDetailsEntity);
        }
        return jobDetailsEntity;
    }
}
