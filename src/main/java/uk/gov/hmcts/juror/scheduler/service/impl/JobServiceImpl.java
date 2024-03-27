package uk.gov.hmcts.juror.scheduler.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.datastore.repository.JobRepository;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.scheduler.service.contracts.SchedulerService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.BusinessRuleValidationException;
import uk.gov.hmcts.juror.standard.service.exceptions.GenericErrorHandlerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
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

    private void throwErrorIfJobDoesNotExist(String jobKey) {
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
        APIJobDetailsEntity jobDetails = getJob(jobKey);
        if (jobDetails.getCronExpression() == null) {
            throw new BusinessRuleValidationException(new NotAScheduledJobError());
        }
        if (schedulerService.isDisabled(jobKey)) {
            throw new BusinessRuleValidationException(new JobAlreadyDisabledError());
        }
        schedulerService.unregister(jobKey);
    }

    @Override
    public void enable(String jobKey) {
        throwErrorIfJobDoesNotExist(jobKey);
        APIJobDetailsEntity jobDetails = getJob(jobKey);
        if (jobDetails.getCronExpression() == null) {
            throw new BusinessRuleValidationException(new NotAScheduledJobError());
        }
        if (schedulerService.isEnabled(jobKey)) {
            throw new BusinessRuleValidationException(new JobAlreadyEnabledError());
        }
        schedulerService.register(jobDetails);
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
            return addEnableDisableProperty(jobDetailsEntity.get());
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
        jobDetailsMapper.assignJobs(jobDetailsEntity);
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
    @SuppressWarnings("PMD.LawOfDemeter")
    public List<APIJobDetailsEntity> getJobs(JobSearchFilter searchFilter) {
        List<Specification<APIJobDetailsEntity>> specifications = new ArrayList<>();


        if (searchFilter.getJobKey() != null) {
            specifications.add(JobRepository.Specs.byJobKey(searchFilter.getJobKey()));
        }
        if (searchFilter.getTags() != null) {
            specifications.add(JobRepository.Specs.byTags(searchFilter.getTags()));
        }
        List<APIJobDetailsEntity> foundJobs =
            jobRepository.findAll(JobRepository.Specs.orderByCreatedOn(Specification.allOf(
                specifications
            )));

        foundJobs.forEach(this::addEnableDisableProperty);

        if (searchFilter.getEnabled() != null) {
            foundJobs.removeIf(job -> !searchFilter.getEnabled().equals(job.getEnabled()));
        }

        if (foundJobs.isEmpty()) {
            throw new NotFoundException("No Jobs found for the provided filter");
        }
        return foundJobs;
    }

    @Override
    @Transactional
    public APIJobDetailsEntity updateJob(String jobKey, APIJobPatch jobPatch) {
        final APIJobDetailsEntity jobDetailsEntity = getJob(jobKey);
        AtomicBoolean requiresReschedule = new AtomicBoolean(false);

        Optional.ofNullable(jobPatch.getInformation()).ifPresent(information -> {
            Optional.ofNullable(information.getName()).ifPresent(jobDetailsEntity::setName);
            Optional.ofNullable(information.getDescription()).ifPresent(jobDetailsEntity::setDescription);
            Optional.ofNullable(information.getTags()).ifPresent(jobDetailsEntity::setTags);
        });

        Optional.ofNullable(jobPatch.getCronExpression()).ifPresent(value -> {
            jobDetailsEntity.setCronExpression(value);
            requiresReschedule.set(true);
        });

        Optional.ofNullable(jobPatch.getMethod()).ifPresent(jobDetailsEntity::setMethod);
        Optional.ofNullable(jobPatch.getUrl()).ifPresent(jobDetailsEntity::setUrl);
        Optional.ofNullable(jobPatch.getHeaders()).ifPresent(jobDetailsEntity::setHeaders);
        Optional.ofNullable(jobPatch.getAuthenticationDefault()).ifPresent(jobDetailsEntity::setAuthenticationDefault);
        Optional.ofNullable(jobPatch.getPayload()).ifPresent(jobDetailsEntity::setPayload);

        Optional.ofNullable(jobPatch.getValidations()).ifPresent(
            validations -> jobDetailsEntity.setValidations(jobDetailsMapper.apiValidationEntityList(validations)));

        Optional.ofNullable(jobPatch.getPostExecutionActions()).ifPresent(
            postActions -> jobDetailsEntity.setPostExecutionActions(jobDetailsMapper.actionEntityList(postActions)));

        APIJobDetailsEntity updatedJobDetailsEntity = save(jobDetailsEntity);
        if (requiresReschedule.get()) {
            schedulerService.unregister(jobKey);
            schedulerService.register(updatedJobDetailsEntity);
        }
        return updatedJobDetailsEntity;
    }

    private APIJobDetailsEntity addEnableDisableProperty(APIJobDetailsEntity apiJobDetailsEntity) {
        apiJobDetailsEntity.setEnabled(schedulerService.isEnabled(apiJobDetailsEntity.getKey()));
        return apiJobDetailsEntity;
    }
}
