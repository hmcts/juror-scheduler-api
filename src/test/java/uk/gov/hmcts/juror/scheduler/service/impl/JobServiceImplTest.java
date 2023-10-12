package uk.gov.hmcts.juror.scheduler.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.datastore.repository.JobRepository;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.SchedulerService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.testSupport.TestSpecification;
import uk.gov.hmcts.juror.scheduler.testSupport.TestUtil;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.BusinessRuleValidationException;
import uk.gov.hmcts.juror.standard.service.exceptions.GenericErrorHandlerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
        JobServiceImpl.class
    }
)
@DisplayName("JobServiceImpl")
class JobServiceImplTest {

    final String jobKey = "JOB123";

    @MockBean
    private SchedulerService schedulerService;
    @MockBean
    private JobRepository jobRepository;
    @MockBean
    private JobDetailsMapper jobDetailsMapper;
    @MockBean
    private TaskService taskService;

    @Autowired
    private JobServiceImpl jobService;


    @DisplayName("public void deleteJob(String jobKey)")
    @Nested
    class DeleteJob {

        @Test
        @DisplayName("Job Exists")
        void positive_job_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            jobService.deleteJob(jobKey);
            verify(taskService, times(1)).deleteAllByJobKey(eq(jobKey));
            verify(jobRepository, times(1)).deleteById(eq(jobKey));
            verify(schedulerService, times(1)).unregister(eq(jobKey));
        }

        @Test
        @DisplayName("Job does not Exists")
        void negative_job_does_not_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.deleteJob(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", notFoundException.getMessage());
            verify(taskService, never()).deleteAllByJobKey(eq(jobKey));
            verify(jobRepository, never()).deleteById(eq(jobKey));
            verify(schedulerService, never()).unregister(eq(jobKey));
        }
    }

    @DisplayName("public boolean doesJobExist(String jobKey)")
    @Nested
    class DoesJobExist {

        @Test
        @DisplayName("Job Exists")
        void positive_job_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            assertTrue(jobService.doesJobExist(jobKey));
        }

        @Test
        @DisplayName("Job does not Exists")
        void positive_job_does_not_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);
            assertFalse(jobService.doesJobExist(jobKey));
        }
    }

    @DisplayName("public void disable(String jobKey)")
    @Nested
    class DisableJob {
        @Test
        @DisplayName("Scheduled Job exists and is enabled")
        void positive_job_exists_is_scheduled_is_enabled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isDisabled(eq(jobKey))).thenReturn(false);

            jobService.disable(jobKey);
            verify(schedulerService, times(1)).isScheduled(eq(jobKey));
            verify(schedulerService, times(1)).isDisabled(eq(jobKey));
            verify(schedulerService, times(1)).disable(eq(jobKey));
        }

        @Test
        @DisplayName("Job does not exist")
        void negative_job_does_not_exist() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isDisabled(eq(jobKey))).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.disable(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", notFoundException.getMessage());

            verify(schedulerService, never()).isScheduled(eq(jobKey));
            verify(schedulerService, never()).isDisabled(eq(jobKey));
            verify(schedulerService, never()).disable(eq(jobKey));
        }

        @Test
        @DisplayName("Job is not scheduled")
        void negative_job_not_scheduled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(false);
            when(schedulerService.isDisabled(eq(jobKey))).thenReturn(false);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.disable(jobKey));
            assertEquals(NotAScheduledJobError.class, exception.getErrorObject().getClass());
            verify(schedulerService, times(1)).isScheduled(eq(jobKey));
            verify(schedulerService, never()).disable(eq(jobKey));
        }

        @Test
        @DisplayName("Job is already disabled")
        void negative_job_already_disabled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isDisabled(eq(jobKey))).thenReturn(true);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.disable(jobKey));
            assertEquals(JobAlreadyDisabledError.class, exception.getErrorObject().getClass());
            verify(schedulerService, times(1)).isDisabled(eq(jobKey));
            verify(schedulerService, never()).disable(eq(jobKey));
        }
    }

    @DisplayName("public void enable(String jobKey)")
    @Nested
    class EnableJob {
        @Test
        @DisplayName("Scheduled Job exists and is disabled")
        void positive_job_exists_is_scheduled_is_disabled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isEnabled(eq(jobKey))).thenReturn(false);

            jobService.enable(jobKey);
            verify(schedulerService, times(1)).isScheduled(eq(jobKey));
            verify(schedulerService, times(1)).isEnabled(eq(jobKey));
            verify(schedulerService, times(1)).enable(eq(jobKey));
        }

        @Test
        @DisplayName("Job does not exist")
        void negative_job_does_not_exist() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isEnabled(eq(jobKey))).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.enable(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", notFoundException.getMessage());

            verify(schedulerService, never()).isScheduled(eq(jobKey));
            verify(schedulerService, never()).isEnabled(eq(jobKey));
            verify(schedulerService, never()).enable(eq(jobKey));
        }

        @Test
        @DisplayName("Job is not scheduled")
        void negative_job_not_scheduled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(false);
            when(schedulerService.isEnabled(eq(jobKey))).thenReturn(false);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.enable(jobKey));
            assertEquals(NotAScheduledJobError.class, exception.getErrorObject().getClass());
            verify(schedulerService, times(1)).isScheduled(eq(jobKey));
            verify(schedulerService, never()).enable(eq(jobKey));
        }

        @Test
        @DisplayName("Job is already enabled")
        void negative_job_already_enabled() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            when(schedulerService.isScheduled(eq(jobKey))).thenReturn(true);
            when(schedulerService.isEnabled(eq(jobKey))).thenReturn(true);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.enable(jobKey));
            assertEquals(JobAlreadyEnabledError.class, exception.getErrorObject().getClass());
            verify(schedulerService, times(1)).isEnabled(eq(jobKey));
            verify(schedulerService, never()).enable(eq(jobKey));
        }
    }


    @DisplayName("public void executeJob(String jobKey)")
    @Nested
    class ExecuteJob {
        @Test
        @DisplayName("Job Exists")
        void positive_job_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);
            jobService.executeJob(jobKey);
            verify(schedulerService, times(1)).executeJob(eq(jobKey));
        }

        @Test
        @DisplayName("Job does not Exists")
        void negative_job_does_not_exists() {
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.executeJob(jobKey));
            assertEquals("Job with key '" + jobKey + "' not found", notFoundException.getMessage());
            verify(schedulerService, never()).executeJob(eq(jobKey));
        }
    }

    @DisplayName("public APIJobDetailsEntity getJob(String key)")
    @Nested
    class GetJob {
        @Test
        @DisplayName("Job Exists")
        void positive_job_exists() {
            Optional<APIJobDetailsEntity> jobDetailsEntityOptional = Optional.of(new APIJobDetailsEntity());
            when(jobRepository.findById(eq(jobKey))).thenReturn(jobDetailsEntityOptional);
            APIJobDetailsEntity apiJobDetailsEntity = jobService.getJob(jobKey);
            assertEquals(jobDetailsEntityOptional.get(), apiJobDetailsEntity);
        }

        @Test
        @DisplayName("Job does not Exists")
        void negative_job_does_not_exists() {
            Optional<APIJobDetailsEntity> jobDetailsEntityOptional = Optional.empty();
            when(jobRepository.findById(eq(jobKey))).thenReturn(jobDetailsEntityOptional);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> jobService.getJob(jobKey));
            assertEquals("Job not found for key: " + jobKey, exception.getMessage());
        }
    }

    @DisplayName("public void createJob(APIJobDetails jobDetails)")
    @Nested
    class CreateJob {
        @Test
        @DisplayName("Scheduled Job Created")
        void positive_scheduled_job_created() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(jobKey);
            apiJobDetails.setCronExpression("* 5 * * * ?");

            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);

            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(jobKey);
            apiJobDetailsEntity.setCronExpression(apiJobDetails.getCronExpression());

            when(jobDetailsMapper.toAPIJobDetailsEntity(eq(apiJobDetails))).thenReturn(apiJobDetailsEntity);
            when(jobRepository.save(eq(apiJobDetailsEntity))).thenReturn(apiJobDetailsEntity);

            jobService.createJob(apiJobDetails);

            verify(jobRepository, times(1)).save(eq(apiJobDetailsEntity));
            verify(schedulerService, times(1)).register(eq(apiJobDetailsEntity));
        }

        @Test
        @DisplayName("Manual Job Created")
        void positive_manual_job_created() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(jobKey);
            apiJobDetails.setCronExpression(null);

            final String jobKey = apiJobDetails.getKey();
            when(jobRepository.existsById(eq(jobKey))).thenReturn(false);

            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(jobKey);
            apiJobDetailsEntity.setCronExpression(null);

            when(jobDetailsMapper.toAPIJobDetailsEntity(eq(apiJobDetails))).thenReturn(apiJobDetailsEntity);
            when(jobRepository.save(eq(apiJobDetailsEntity))).thenReturn(apiJobDetailsEntity);

            jobService.createJob(apiJobDetails);

            verify(jobRepository, times(1)).save(eq(apiJobDetailsEntity));
            verify(schedulerService, never()).register(any());

        }

        @Test
        @DisplayName("Job with key already exists")
        void negative_job_with_same_key_exists() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(jobKey);
            when(jobRepository.existsById(eq(jobKey))).thenReturn(true);


            GenericErrorHandlerException exception = assertThrows(GenericErrorHandlerException.class, () ->
                jobService.createJob(apiJobDetails));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals(APIHandleableException.Type.INFORMATIONAL, exception.getExceptionType());
            assertNotNull(exception.getErrorObject());
            assertEquals(KeyAlreadyInUseError.class, exception.getErrorObject().getClass());

            verify(jobRepository, never()).save(any());
            verify(schedulerService, never()).register(any());
        }
    }

    @DisplayName("public APIJobDetailsEntity save(APIJobDetailsEntity jobDetailsEntity)")
    @Nested
    class Save {
        @Test
        @DisplayName("Job saved")
        void positive_save_job_entity() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(jobKey);
            APIJobDetailsEntity savedApiJobDetailsEntity = new APIJobDetailsEntity();
            savedApiJobDetailsEntity.setKey(jobKey);
            savedApiJobDetailsEntity.setUrl("www.savedUrl.com");//This has no processing effect but rather is to
            // ensure the assertEquals has two unique objects to compare too
            when(jobRepository.save(eq(apiJobDetailsEntity))).thenReturn(savedApiJobDetailsEntity);
            Assertions.assertEquals(savedApiJobDetailsEntity, jobService.save(apiJobDetailsEntity));

            verify(jobRepository, times(1)).save(eq(apiJobDetailsEntity));
        }
    }

    @DisplayName("public List<APIJobDetailsEntity> getJobs(JobSearchFilter searchFilter)")
    @Nested
    class GetJobs {

        @Captor
        private ArgumentCaptor<List<Specification<APIJobDetailsEntity>>> captor;

        @Test
        @DisplayName("No filter")
        void positive_get_jobs_no_filter() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                JobSearchFilter jobSearchFilter = JobSearchFilter.builder().build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs);
                    assertEquals(0, specs.size());
                    assertEquals(jobs.size(), returnedJobs.size());
                    assertThat(returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(any()), never());
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Job Key Filter")
        void positive_get_jobs_kob_key_filter() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                JobSearchFilter jobSearchFilter = JobSearchFilter.builder()
                    .jobKey(jobKey).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs);
                    assertEquals(1, specs.size());
                    Assertions.assertEquals("byJobKey", ((TestSpecification) specs.get(0)).getName());

                    assertEquals(jobs.size(), returnedJobs.size());
                    assertThat(returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(eq(jobKey)), times(1));
                utilities.verify(() -> JobRepository.Specs.byTags(any()), never());
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Single Tag Filter")
        void positive_get_jobs_single_tag_filter() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                Set<String> tags = new HashSet<>();
                tags.add("MyTag");
                JobSearchFilter jobSearchFilter = JobSearchFilter.builder()
                    .tags(tags).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs);
                    assertEquals(1, specs.size());
                    assertEquals("byTags", ((TestSpecification) specs.get(0)).getName());

                    assertEquals(jobs.size(), returnedJobs.size());
                    assertThat(returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(eq(tags)), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple Tag Filter")
        void positive_get_jobs_multiple_tag_filter() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                Set<String> tags = new HashSet<>();
                tags.add("MyTag");
                tags.add("MyTag2");
                tags.add("MyTag3");
                JobSearchFilter jobSearchFilter = JobSearchFilter.builder()
                    .tags(tags).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs);
                    assertEquals(1, specs.size());
                    assertEquals("byTags", ((TestSpecification) specs.get(0)).getName());

                    assertEquals(jobs.size(), returnedJobs.size());
                    assertThat(returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(eq(tags)), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Get Jobs - Multiple filters")
        void positive_get_jobs_multiple_filter_types() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                Set<String> tags = new HashSet<>();
                tags.add("MyTag");
                tags.add("MyTag2");
                tags.add("MyTag3");
                JobSearchFilter jobSearchFilter = JobSearchFilter.builder()
                    .jobKey(jobKey)
                    .tags(tags).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs);
                    assertEquals(2, specs.size());
                    assertEquals("byJobKey", ((TestSpecification) specs.get(0)).getName());
                    assertEquals("byTags", ((TestSpecification) specs.get(1)).getName());

                    assertEquals(jobs.size(), returnedJobs.size());
                    assertThat(returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(eq(jobKey)), times(1));
                utilities.verify(() -> JobRepository.Specs.byTags(eq(tags)), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("No Jobs found")
        void negative_not_jobs_found() {

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(Collections.emptyList());
            JobSearchFilter jobSearchFilter = JobSearchFilter.builder().build();
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                jobService.getJobs(jobSearchFilter);
            });

            assertEquals("No Jobs found for the provided filter", exception.getMessage());
        }

        private void setupSpecificationMocks(MockedStatic<JobRepository.Specs> utilities){
            utilities.when(() -> JobRepository.Specs.byJobKey(any()))
                .thenReturn(new TestSpecification("byJobKey"));
            utilities.when(() -> JobRepository.Specs.byTags(any()))
                .thenReturn(new TestSpecification("byTags"));
            utilities.when(() -> JobRepository.Specs.byCreateDateGreaterThan(any()))
                .thenReturn(new TestSpecification("byCreateDateGreaterThan"));
            utilities.when(() -> JobRepository.Specs.orderByCreatedOn(any()))
                .thenReturn(new TestSpecification("orderByCreatedOn"));

        }
    }


    @DisplayName("public APIJobDetailsEntity updateJob(String jobKey, APIJobPatch jobPatch)")
    @Nested
    class UpdateJob {

        private void triggerAndValidateUpdate(APIJobPatch apiJobPatch) {
            try {
                final APIJobDetailsEntity baseApiJobDetailsEntity = TestUtil.generateAPIJobDetailsEntry();
                APIJobDetailsEntity apiJobDetailsEntity = TestUtil.cloneAPIJobDetailsEntity(baseApiJobDetailsEntity);

                when(jobRepository.findById(eq(jobKey))).thenReturn(Optional.of(apiJobDetailsEntity));
                when(jobRepository.save(eq(apiJobDetailsEntity))).thenReturn(apiJobDetailsEntity);

                List<APIValidationEntity> convertedValidations = Collections.emptyList();
                if (apiJobPatch.getValidations() != null) {
                    convertedValidations = TestUtil.convertValidations(apiJobPatch.getValidations());
                    when(jobDetailsMapper.apiValidationEntityList(eq(apiJobPatch.getValidations()))).thenReturn(convertedValidations);
                }
                APIJobDetailsEntity updateAPIApiJobDetails = jobService.updateJob(jobKey, apiJobPatch);

                //Information
                if (apiJobPatch.getInformation() != null) {
                    assertEquals(updateAPIApiJobDetails.getName(),
                        Optional.ofNullable(apiJobPatch.getInformation().getName()).orElseGet(baseApiJobDetailsEntity::getName));
                    assertEquals(updateAPIApiJobDetails.getDescription(),
                        Optional.ofNullable(apiJobPatch.getInformation().getDescription()).orElseGet(baseApiJobDetailsEntity::getDescription));
                    assertEquals(updateAPIApiJobDetails.getTags(),
                        Optional.ofNullable(apiJobPatch.getInformation().getTags()).orElseGet(baseApiJobDetailsEntity::getTags));
                } else {
                    assertEquals(updateAPIApiJobDetails.getName(), baseApiJobDetailsEntity.getName());
                    assertEquals(updateAPIApiJobDetails.getDescription(), baseApiJobDetailsEntity.getDescription());
                    assertEquals(updateAPIApiJobDetails.getTags(), baseApiJobDetailsEntity.getTags());
                }

                assertEquals(updateAPIApiJobDetails.getCronExpression(),
                    Optional.ofNullable(apiJobPatch.getCronExpression()).orElseGet(baseApiJobDetailsEntity::getCronExpression));
                assertEquals(updateAPIApiJobDetails.getMethod(),
                    Optional.ofNullable(apiJobPatch.getMethod()).orElseGet(baseApiJobDetailsEntity::getMethod));
                assertEquals(updateAPIApiJobDetails.getUrl(),
                    Optional.ofNullable(apiJobPatch.getUrl()).orElseGet(baseApiJobDetailsEntity::getUrl));
                assertEquals(updateAPIApiJobDetails.getHeaders(),
                    Optional.ofNullable(apiJobPatch.getHeaders()).orElseGet(baseApiJobDetailsEntity::getHeaders));
                assertEquals(updateAPIApiJobDetails.getAuthenticationDefault(),
                    Optional.ofNullable(apiJobPatch.getAuthenticationDefault()).orElseGet(baseApiJobDetailsEntity::getAuthenticationDefault));
                assertEquals(updateAPIApiJobDetails.getPayload(),
                    Optional.ofNullable(apiJobPatch.getPayload()).orElseGet(baseApiJobDetailsEntity::getPayload));

                if (apiJobPatch.getValidations() == null) {
                    validateValidations(baseApiJobDetailsEntity.getValidations(),
                        updateAPIApiJobDetails.getValidations());
                } else {
                    validateValidations(convertedValidations, updateAPIApiJobDetails.getValidations());
                }

                verify(jobRepository, times(1)).save(eq(apiJobDetailsEntity));
                if (apiJobPatch.getCronExpression() != null) {
                    verify(schedulerService, times(1)).unregister(eq(jobKey));
                    verify(schedulerService, times(1)).register(eq(apiJobDetailsEntity));
                } else {

                    verify(schedulerService, never()).unregister(any());
                    verify(schedulerService, never()).register(any());
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private void validateValidations(List<APIValidationEntity> expected, List<APIValidationEntity> actual) {
            int index = 0;
            assertEquals(expected.size(), actual.size());
            for (APIValidationEntity entity : expected) {
                APIValidationEntity validationActual = actual.get(index++);
                if (entity instanceof StatusCodeValidationEntity statusCodeAPIValidationExpected) {
                    assertInstanceOf(StatusCodeValidationEntity.class, validationActual);
                    StatusCodeValidationEntity statusCodeValidationActual =
                        (StatusCodeValidationEntity) validationActual;
                    assertEquals(statusCodeAPIValidationExpected.getExpectedStatusCode(),
                        statusCodeValidationActual.getExpectedStatusCode());
                } else if (entity instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidationExpected) {
                    assertInstanceOf(MaxResponseTimeAPIValidationEntity.class, maxResponseTimeAPIValidationExpected);
                    MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidationActual =
                        (MaxResponseTimeAPIValidationEntity) validationActual;
                    assertEquals(maxResponseTimeAPIValidationExpected.getMaxResponseTimeMS(),
                        maxResponseTimeAPIValidationActual.getMaxResponseTimeMS());
                } else if (entity instanceof JsonPathAPIValidationEntity jsonPathAPIValidationExpected) {
                    assertInstanceOf(JsonPathAPIValidationEntity.class, validationActual);
                    JsonPathAPIValidationEntity jsonPathAPIValidationActual =
                        (JsonPathAPIValidationEntity) validationActual;
                    assertEquals(jsonPathAPIValidationExpected.getPath(),
                        jsonPathAPIValidationActual.getPath());
                    assertEquals(jsonPathAPIValidationExpected.getExpectedResponse(),
                        jsonPathAPIValidationActual.getExpectedResponse());
                } else {
                    fail("Unknown validation type: " + validationActual.getClass());
                }
            }
        }

        @Test
        @DisplayName("Information - Name")
        void positive_update_information_name() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .information(Information.builder()
                    .name("New Name").build())
                .build());
        }

        @Test
        @DisplayName("Information - Description")
        void positive_update_information_description() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .information(Information.builder()
                    .description("New Desc").build())
                .build());

        }

        @Test
        @DisplayName("Information - Tags")
        void positive_update_information_tags() {
            Set<String> tags = new HashSet<>();
            tags.add("New Tag 1");
            tags.add("New Tag 2");
            tags.add("New Tag 3");
            triggerAndValidateUpdate(APIJobPatch.builder()
                .information(Information.builder()
                    .tags(tags).build())
                .build());
        }


        @Test
        @DisplayName("Cron Expression")
        void positive_update_cron_expression() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .cronExpression("8 7 * * * ?")
                .build());
        }

        @Test
        @DisplayName("Method")
        void positive_update_method() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .method(APIMethod.TRACE)
                .build());

        }

        @Test
        @DisplayName("URL")
        void positive_update_url() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .url("https://google.com")
                .build());
        }

        @Test
        @DisplayName("Headers")
        void positive_update_headers() {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("key1", "value1");
            headers.put("key2", "value2");
            headers.put("key3", "value3");
            triggerAndValidateUpdate(APIJobPatch.builder()
                .headers(headers)
                .build());
        }

        @Test
        @DisplayName("Authentication Default")
        void positive_update_authentication_default() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .authenticationDefault(AuthenticationDefaults.NONE)
                .build());
        }

        @Test
        @DisplayName("Payload")
        void positive_update_payload() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .payload("New Payload")
                .build());
        }

        @Test
        @DisplayName("Validations")
        void positive_update_validations() {
            List<APIValidation> validationEntities = new ArrayList<>();
            validationEntities.add(new StatusCodeAPIValidation(499));
            validationEntities.add(new JsonPathAPIValidation("$.name", "jerry"));
            validationEntities.add(new MaxResponseTimeAPIValidation(2000));
            triggerAndValidateUpdate(APIJobPatch.builder()
                .validations(validationEntities)
                .build());
        }
    }
}
