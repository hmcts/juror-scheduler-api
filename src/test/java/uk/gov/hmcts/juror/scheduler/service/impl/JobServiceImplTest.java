package uk.gov.hmcts.juror.scheduler.service.impl;

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
import uk.gov.hmcts.juror.scheduler.api.model.error.KeyAlreadyInUseError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyEnabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.Information;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.Action;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobPatch;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.APIMethod;
import uk.gov.hmcts.juror.scheduler.datastore.model.AuthenticationDefaults;
import uk.gov.hmcts.juror.scheduler.datastore.model.filter.JobSearchFilter;
import uk.gov.hmcts.juror.scheduler.datastore.repository.JobRepository;
import uk.gov.hmcts.juror.scheduler.mapping.JobDetailsMapper;
import uk.gov.hmcts.juror.scheduler.service.contracts.SchedulerService;
import uk.gov.hmcts.juror.scheduler.service.contracts.TaskService;
import uk.gov.hmcts.juror.scheduler.testsupport.TestSpecification;
import uk.gov.hmcts.juror.scheduler.testsupport.util.CloneUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.ConvertUtil;
import uk.gov.hmcts.juror.scheduler.testsupport.util.GenerateUtil;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods"
})
class JobServiceImplTest {

    private static final String JOB_KEY = "JOB123";

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
        void positiveJobExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            jobService.deleteJob(JOB_KEY);
            verify(taskService, times(1)).deleteAllByJobKey(JOB_KEY);
            verify(jobRepository, times(1)).deleteById(JOB_KEY);
            verify(schedulerService, times(1)).unregister(JOB_KEY);
        }

        @Test
        @DisplayName("Job does not Exists")
        void negativeJobDoesNotExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.deleteJob(JOB_KEY));
            assertEquals("Job with key '" + JOB_KEY + "' not found",
                notFoundException.getMessage(), "Message must match");
            verify(taskService, never()).deleteAllByJobKey(JOB_KEY);
            verify(jobRepository, never()).deleteById(JOB_KEY);
            verify(schedulerService, never()).unregister(JOB_KEY);
        }
    }

    @DisplayName("public boolean doesJobExist(String jobKey)")
    @Nested
    class DoesJobExist {

        @Test
        @DisplayName("Job Exists")
        void positiveJobExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            assertTrue(jobService.doesJobExist(JOB_KEY), "Job should exist");
        }

        @Test
        @DisplayName("Job does not Exists")
        void positiveJobDoesNotExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(false);
            assertFalse(jobService.doesJobExist(JOB_KEY), "Job should exist");
        }
    }

    @DisplayName("public void disable(String jobKey)")
    @Nested
    class DisableJob {
        @Test
        @DisplayName("Scheduled Job exists and is enabled")
        void positiveJobExistsIsScheduledIsEnabled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            when(jobDetailsEntity.getCronExpression()).thenReturn("* 5 * * * ?");
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);

            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(true);
            when(schedulerService.isDisabled(JOB_KEY)).thenReturn(false);

            jobService.disable(JOB_KEY);
            verify(jobService, times(1)).getJob(JOB_KEY);
            verify(schedulerService, times(1)).isDisabled(any());
            verify(schedulerService, times(1)).unregister(any());
        }

        @Test
        @DisplayName("Job does not exist")
        void negativeJobDoesNotExist() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(false);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(true);
            when(schedulerService.isDisabled(JOB_KEY)).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.disable(JOB_KEY));
            assertEquals("Job with key '" + JOB_KEY + "' not found",
                notFoundException.getMessage(), "Message must match");

            verify(schedulerService, never()).isScheduled(any());
            verify(schedulerService, never()).isDisabled(any());
            verify(schedulerService, never()).unregister(any());
        }

        @Test
        @DisplayName("Job is not scheduled")
        void negativeJobNotScheduled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);

            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(false);
            when(schedulerService.isDisabled(JOB_KEY)).thenReturn(false);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.disable(JOB_KEY));
            assertEquals(NotAScheduledJobError.class, exception.getErrorObject().getClass(), "Class must match");
            verify(schedulerService, never()).unregister(JOB_KEY);
            verify(jobService,times(1)).getJob(JOB_KEY);
        }

        @Test
        @DisplayName("Job is already disabled")
        void negativeJobAlreadyDisabled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            when(jobDetailsEntity.getCronExpression()).thenReturn("* 5 * * * ?");
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);

            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(true);
            when(schedulerService.isDisabled(JOB_KEY)).thenReturn(true);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.disable(JOB_KEY));
            assertEquals(JobAlreadyDisabledError.class, exception.getErrorObject().getClass(), "Class must match");
            verify(schedulerService, times(1)).isDisabled(JOB_KEY);
            verify(schedulerService, never()).unregister(JOB_KEY);
            verify(jobService,times(1)).getJob(JOB_KEY);
        }
    }

    @DisplayName("public void enable(String jobKey)")
    @Nested
    class EnableJob {
        @Test
        @DisplayName("Scheduled Job exists and is disabled")
        void positiveJobExistsIsScheduledIsDisabled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            when(jobDetailsEntity.getCronExpression()).thenReturn("* 5 * * * ?");
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);

            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(true);
            when(schedulerService.isEnabled(JOB_KEY)).thenReturn(false);

            jobService.enable(JOB_KEY);
            verify(schedulerService, times(1)).isEnabled(JOB_KEY);
            verify(schedulerService, times(1)).register(jobDetailsEntity);
            verify(jobService,times(1)).getJob(JOB_KEY);
        }

        @Test
        @DisplayName("Job is not scheduled")
        void negativeJobNotScheduled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(false);
            when(schedulerService.isEnabled(JOB_KEY)).thenReturn(false);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.enable(JOB_KEY));
            assertEquals(NotAScheduledJobError.class, exception.getErrorObject().getClass(), "Class must match");
            verify(schedulerService, never()).register(jobDetailsEntity);
            verify(jobService,times(1)).getJob(JOB_KEY);
        }

        @Test
        @DisplayName("Job is already enabled")
        void negativeJobAlreadyEnabled() {
            jobService = spy(jobService);
            APIJobDetailsEntity jobDetailsEntity = mock(APIJobDetailsEntity.class);
            when(jobDetailsEntity.getCronExpression()).thenReturn("* 5 * * * ?");
            doReturn(jobDetailsEntity).when(jobService).getJob(JOB_KEY);
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            when(schedulerService.isScheduled(JOB_KEY)).thenReturn(true);
            when(schedulerService.isEnabled(JOB_KEY)).thenReturn(true);

            BusinessRuleValidationException exception = assertThrows(BusinessRuleValidationException.class,
                () -> jobService.enable(JOB_KEY));
            assertEquals(JobAlreadyEnabledError.class, exception.getErrorObject().getClass(), "Class must match");
            verify(schedulerService, times(1)).isEnabled(JOB_KEY);
            verify(schedulerService, never()).register(jobDetailsEntity);
            verify(jobService,times(1)).getJob(JOB_KEY);
        }
    }


    @DisplayName("public void executeJob(String jobKey)")
    @Nested
    class ExecuteJob {
        @Test
        @DisplayName("Job Exists")
        void positiveJobExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);
            jobService.executeJob(JOB_KEY);
            verify(schedulerService, times(1)).executeJob(JOB_KEY);
        }

        @Test
        @DisplayName("Job does not Exists")
        void negativeJobDoesNotExists() {
            when(jobRepository.existsById(JOB_KEY)).thenReturn(false);
            NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> jobService.executeJob(JOB_KEY));
            assertEquals("Job with key '" + JOB_KEY + "' not found", notFoundException.getMessage(),
                "Message must match");
            verify(schedulerService, never()).executeJob(JOB_KEY);
        }
    }

    @DisplayName("public APIJobDetailsEntity getJob(String key)")
    @Nested
    class GetJob {
        @Test
        @DisplayName("Job Exists")
        void positiveJobExists() {
            Optional<APIJobDetailsEntity> jobDetailsEntityOptional = Optional.of(new APIJobDetailsEntity());
            when(jobRepository.findById(JOB_KEY)).thenReturn(jobDetailsEntityOptional);
            APIJobDetailsEntity apiJobDetailsEntity = jobService.getJob(JOB_KEY);
            assertEquals(jobDetailsEntityOptional.get(), apiJobDetailsEntity, "JobDetailsEntity must match");
        }

        @Test
        @DisplayName("Job does not Exists")
        void negativeJobDoesNotExists() {
            Optional<APIJobDetailsEntity> jobDetailsEntityOptional = Optional.empty();
            when(jobRepository.findById(JOB_KEY)).thenReturn(jobDetailsEntityOptional);

            NotFoundException exception = assertThrows(NotFoundException.class, () -> jobService.getJob(JOB_KEY));
            assertEquals("Job not found for key: " + JOB_KEY, exception.getMessage(), "Message must match");
        }
    }

    @DisplayName("public void createJob(APIJobDetails jobDetails)")
    @Nested
    class CreateJob {
        @Test
        @DisplayName("Scheduled Job Created")
        void positiveScheduledJobCreated() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(JOB_KEY);
            apiJobDetails.setCronExpression("* 5 * * * ?");

            when(jobRepository.existsById(JOB_KEY)).thenReturn(false);

            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(JOB_KEY);
            apiJobDetailsEntity.setCronExpression(apiJobDetails.getCronExpression());

            when(jobDetailsMapper.toAPIJobDetailsEntity(apiJobDetails)).thenReturn(apiJobDetailsEntity);
            when(jobRepository.save(apiJobDetailsEntity)).thenReturn(apiJobDetailsEntity);

            jobService.createJob(apiJobDetails);

            verify(jobRepository, times(1)).save(apiJobDetailsEntity);
            verify(schedulerService, times(1)).register(apiJobDetailsEntity);
        }

        @Test
        @DisplayName("Manual Job Created")
        void positiveManualJobCreated() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(JOB_KEY);
            apiJobDetails.setCronExpression(null);

            final String jobKey = apiJobDetails.getKey();
            when(jobRepository.existsById(jobKey)).thenReturn(false);

            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(jobKey);
            apiJobDetailsEntity.setCronExpression(null);

            when(jobDetailsMapper.toAPIJobDetailsEntity(apiJobDetails)).thenReturn(apiJobDetailsEntity);
            when(jobRepository.save(apiJobDetailsEntity)).thenReturn(apiJobDetailsEntity);

            jobService.createJob(apiJobDetails);

            verify(jobRepository, times(1)).save(apiJobDetailsEntity);
            verify(schedulerService, never()).register(any());

        }

        @Test
        @DisplayName("Job with key already exists")
        void negativeJobWithSameKeyExists() {
            APIJobDetails apiJobDetails = new APIJobDetails();
            apiJobDetails.setKey(JOB_KEY);
            when(jobRepository.existsById(JOB_KEY)).thenReturn(true);


            GenericErrorHandlerException exception = assertThrows(GenericErrorHandlerException.class, () ->
                jobService.createJob(apiJobDetails));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode(), "Status must match");
            assertEquals(APIHandleableException.Type.INFORMATIONAL, exception.getExceptionType(),
                "Exception type must match");
            assertNotNull(exception.getErrorObject(), "Error object must not be null");
            assertEquals(KeyAlreadyInUseError.class, exception.getErrorObject().getClass(), "Class must match");

            verify(jobRepository, never()).save(any());
            verify(schedulerService, never()).register(any());
        }
    }

    @DisplayName("public APIJobDetailsEntity save(APIJobDetailsEntity jobDetailsEntity)")
    @Nested
    class SaveMethod {
        @Test
        @DisplayName("Job saved")
        void positiveSaveJobEntity() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.setKey(JOB_KEY);
            APIJobDetailsEntity savedApiJobDetailsEntity = new APIJobDetailsEntity();
            savedApiJobDetailsEntity.setKey(JOB_KEY);
            savedApiJobDetailsEntity.setUrl("www.savedUrl.com");//This has no processing effect but rather is to
            // ensure the assertEquals has two unique objects to compare too
            when(jobRepository.save(apiJobDetailsEntity)).thenReturn(savedApiJobDetailsEntity);
            assertEquals(savedApiJobDetailsEntity, jobService.save(apiJobDetailsEntity), "Saved entity must match");

            verify(jobRepository, times(1)).save(apiJobDetailsEntity);
        }
    }

    @DisplayName("public List<APIJobDetailsEntity> getJobs(JobSearchFilter searchFilter)")
    @Nested
    class GetJobs {

        @Captor
        private ArgumentCaptor<List<Specification<APIJobDetailsEntity>>> captor;

        @Test
        @DisplayName("No filter")
        void positiveGetJobsNoFilter() {
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
                    assertEquals(jobs.size(), returnedJobs.size(), "Returned jobs size must match");
                    assertThat("Returned Jobs should match", returnedJobs,
                        hasItems(jobs.toArray(new APIJobDetailsEntity[0])));

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs, "Specs should not be null");
                    assertEquals(0, specs.size(), "Spec Size must be 0");
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(any()), never());
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Job Key Filter")
        void positiveGetJobsKobKeyFilter() {
            List<APIJobDetailsEntity> jobs = new ArrayList<>();
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());
            jobs.add(new APIJobDetailsEntity());

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(jobs);
            try (MockedStatic<JobRepository.Specs> utilities = Mockito.mockStatic(JobRepository.Specs.class)) {
                setupSpecificationMocks(utilities);

                JobSearchFilter jobSearchFilter = JobSearchFilter.builder()
                    .jobKey(JOB_KEY).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    assertEquals(jobs.size(), returnedJobs.size(), "Returned jobs size must match");
                    assertThat("Returned jobs match", returnedJobs, hasItems(jobs.toArray(new APIJobDetailsEntity[0])));

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs, "Specs should not be null");
                    assertEquals(1, specs.size(), "Spec size must match");
                    assertEquals("byJobKey", ((TestSpecification) specs.get(0)).name(), "Spec name must match");
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(JOB_KEY), times(1));
                utilities.verify(() -> JobRepository.Specs.byTags(any()), never());
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Single Tag Filter")
        void positiveGetJobsSingleTagFilter() {
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

                    assertEquals(jobs.size(), returnedJobs.size(), "Returned job size must match");
                    assertThat("Returned jobs should match", returnedJobs,
                        hasItems(jobs.toArray(new APIJobDetailsEntity[0])));

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs, "Specs should not be null");
                    assertEquals(1, specs.size(), "Size size must match");
                    assertEquals("byTags", ((TestSpecification) specs.get(0)).name(), "Spec name must match");
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(tags), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Multiple Tag Filter")
        void positiveGetJobsMultipleTagFilter() {
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

                    assertEquals(jobs.size(), returnedJobs.size(), "Returned jobs size must match");
                    assertThat("Returned jobs should match", returnedJobs,
                        hasItems(jobs.toArray(new APIJobDetailsEntity[0])));

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs, "Specs should not be null");
                    assertEquals(1, specs.size(), "Spec size must match");
                    assertEquals("byTags", ((TestSpecification) specs.get(0)).name(), "Spec name must match");
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(any()), never());
                utilities.verify(() -> JobRepository.Specs.byTags(tags), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("Get Jobs - Multiple filters")
        void positiveGetJobsMultipleFilterTypes() {
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
                    .jobKey(JOB_KEY)
                    .tags(tags).build();

                try (MockedStatic<Specification> specificationMockedStatic = Mockito.mockStatic(Specification.class)) {

                    List<APIJobDetailsEntity> returnedJobs = jobService.getJobs(jobSearchFilter);

                    assertEquals(jobs.size(), returnedJobs.size(), "Returned job size must match");
                    assertThat("Returned jobs should match", returnedJobs,
                        hasItems(jobs.toArray(new APIJobDetailsEntity[0])));

                    specificationMockedStatic.verify(() -> Specification.allOf(captor.capture()));

                    List<Specification<APIJobDetailsEntity>> specs = captor.getValue();
                    assertNotNull(specs, "Specs should not be null");
                    assertEquals(2, specs.size(), "Spec size must match");
                    assertEquals("byJobKey", ((TestSpecification) specs.get(0)).name(), "Spec name must match");
                    assertEquals("byTags", ((TestSpecification) specs.get(1)).name(), "Spec name must match");
                }
                utilities.verify(() -> JobRepository.Specs.byJobKey(JOB_KEY), times(1));
                utilities.verify(() -> JobRepository.Specs.byTags(tags), times(1));
                utilities.verify(() -> JobRepository.Specs.byCreateDateGreaterThan(any()), never());
                utilities.verify(() -> JobRepository.Specs.orderByCreatedOn(any()), times(1));
            }
        }

        @Test
        @DisplayName("No Jobs found")
        void negativeNotJobsFound() {

            when(jobRepository.findAll(ArgumentMatchers.<Specification<APIJobDetailsEntity>>any())).thenReturn(
                Collections.emptyList());
            JobSearchFilter jobSearchFilter = JobSearchFilter.builder().build();
            NotFoundException exception = assertThrows(NotFoundException.class, () -> {
                jobService.getJobs(jobSearchFilter);
            });

            assertEquals("No Jobs found for the provided filter", exception.getMessage(),
                "Message must match");
        }

        private void setupSpecificationMocks(MockedStatic<JobRepository.Specs> utilities) {
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

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        private void triggerAndValidateUpdate(APIJobPatch apiJobPatch) {
            try {
                final APIJobDetailsEntity baseApiJobDetailsEntity = GenerateUtil.generateAPIJobDetailsEntry();
                APIJobDetailsEntity apiJobDetailsEntity = CloneUtil.cloneAPIJobDetailsEntity(baseApiJobDetailsEntity);

                when(jobRepository.findById(JOB_KEY)).thenReturn(Optional.of(apiJobDetailsEntity));
                when(jobRepository.save(apiJobDetailsEntity)).thenReturn(apiJobDetailsEntity);

                List<APIValidationEntity> convertedValidations = Collections.emptyList();
                if (apiJobPatch.getValidations() != null) {
                    convertedValidations = ConvertUtil.convertValidations(apiJobPatch.getValidations());
                    when(jobDetailsMapper.apiValidationEntityList(apiJobPatch.getValidations())).thenReturn(
                        convertedValidations);
                }
                List<ActionEntity> convertedPostActions = Collections.emptyList();
                if (apiJobPatch.getPostExecutionActions() != null) {
                    convertedPostActions = ConvertUtil.convertPostActions(apiJobPatch.getPostExecutionActions());
                    when(jobDetailsMapper.actionEntityList(apiJobPatch.getPostExecutionActions())).thenReturn(
                        convertedPostActions);
                }


                APIJobDetailsEntity updateAPIApiJobDetails = jobService.updateJob(JOB_KEY, apiJobPatch);

                //Information
                if (apiJobPatch.getInformation() != null) {
                    assertEquals(updateAPIApiJobDetails.getName(),
                        Optional.ofNullable(apiJobPatch.getInformation().getName())
                            .orElseGet(baseApiJobDetailsEntity::getName), "Name must match");
                    assertEquals(updateAPIApiJobDetails.getDescription(),
                        Optional.ofNullable(apiJobPatch.getInformation().getDescription())
                            .orElseGet(baseApiJobDetailsEntity::getDescription), "Description must match");
                    assertEquals(updateAPIApiJobDetails.getTags(),
                        Optional.ofNullable(apiJobPatch.getInformation().getTags())
                            .orElseGet(baseApiJobDetailsEntity::getTags), "Tags must match");
                } else {
                    assertEquals(updateAPIApiJobDetails.getName(), baseApiJobDetailsEntity.getName(),
                        "Name must match");
                    assertEquals(updateAPIApiJobDetails.getDescription(),
                        baseApiJobDetailsEntity.getDescription(), "Description must match");
                    assertEquals(updateAPIApiJobDetails.getTags(), baseApiJobDetailsEntity.getTags(),
                        "Tags must match");
                }

                assertEquals(updateAPIApiJobDetails.getCronExpression(),
                    Optional.ofNullable(apiJobPatch.getCronExpression())
                        .orElseGet(baseApiJobDetailsEntity::getCronExpression), "Cron expression must match");
                assertEquals(updateAPIApiJobDetails.getMethod(),
                    Optional.ofNullable(apiJobPatch.getMethod()).orElseGet(baseApiJobDetailsEntity::getMethod),
                    "Method must match");
                assertEquals(updateAPIApiJobDetails.getUrl(),
                    Optional.ofNullable(apiJobPatch.getUrl()).orElseGet(baseApiJobDetailsEntity::getUrl),
                    "Url must match");
                assertEquals(updateAPIApiJobDetails.getHeaders(),
                    Optional.ofNullable(apiJobPatch.getHeaders()).orElseGet(baseApiJobDetailsEntity::getHeaders),
                    "Headers must match");
                assertEquals(updateAPIApiJobDetails.getAuthenticationDefault(),
                    Optional.ofNullable(apiJobPatch.getAuthenticationDefault())
                        .orElseGet(baseApiJobDetailsEntity::getAuthenticationDefault),
                    "Authentication default must match");
                assertEquals(updateAPIApiJobDetails.getPayload(),
                    Optional.ofNullable(apiJobPatch.getPayload()).orElseGet(baseApiJobDetailsEntity::getPayload),
                    "Payload must match");

                if (apiJobPatch.getValidations() == null) {
                    validateValidations(baseApiJobDetailsEntity.getValidations(),
                        updateAPIApiJobDetails.getValidations());
                } else {
                    validateValidations(convertedValidations, updateAPIApiJobDetails.getValidations());
                }

                if (apiJobPatch.getPostExecutionActions() == null) {
                    validatePostActions(baseApiJobDetailsEntity.getPostExecutionActions(),
                        updateAPIApiJobDetails.getPostExecutionActions());
                } else {
                    validatePostActions(convertedPostActions, updateAPIApiJobDetails.getPostExecutionActions());
                }

                verify(jobRepository, times(1)).save(apiJobDetailsEntity);
                if (apiJobPatch.getCronExpression() != null) {
                    verify(schedulerService, times(1)).unregister(JOB_KEY);
                    verify(schedulerService, times(1)).register(apiJobDetailsEntity);
                } else {

                    verify(schedulerService, never()).unregister(any());
                    verify(schedulerService, never()).register(any());
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void validatePostActions(List<ActionEntity> expected, List<ActionEntity> actual) {
            int index = 0;
            assertEquals(expected.size(), actual.size(), "Size must match");
            for (ActionEntity entity : expected) {
                ActionEntity validationActual = actual.get(index++);
                if (entity instanceof RunJobActionEntity runJobActionExpected) {
                    assertInstanceOf(RunJobActionEntity.class, validationActual);
                    RunJobActionEntity runJobActionActual =
                        (RunJobActionEntity) validationActual;
                    assertEquals(runJobActionExpected.getJobKey(), runJobActionActual.getJobKey(),
                        "Job Key must match");
                } else {
                    fail("Unknown action type: " + validationActual.getClass());
                }
            }
        }


        private void validateValidations(List<APIValidationEntity> expected, List<APIValidationEntity> actual) {
            int index = 0;
            assertEquals(expected.size(), actual.size(), "Size must match");
            for (APIValidationEntity entity : expected) {
                APIValidationEntity validationActual = actual.get(index++);
                if (entity instanceof StatusCodeValidationEntity statusCodeAPIValidationExpected) {
                    assertInstanceOf(StatusCodeValidationEntity.class, validationActual);
                    StatusCodeValidationEntity statusCodeValidationActual =
                        (StatusCodeValidationEntity) validationActual;
                    assertEquals(statusCodeAPIValidationExpected.getExpectedStatusCode(),
                        statusCodeValidationActual.getExpectedStatusCode(), "Status code must match");
                } else if (entity instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidationExpected) {
                    assertInstanceOf(MaxResponseTimeAPIValidationEntity.class, maxResponseTimeAPIValidationExpected);
                    MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidationActual =
                        (MaxResponseTimeAPIValidationEntity) validationActual;
                    assertEquals(maxResponseTimeAPIValidationExpected.getMaxResponseTimeMS(),
                        maxResponseTimeAPIValidationActual.getMaxResponseTimeMS(), "Max response time ms must match");
                } else if (entity instanceof JsonPathAPIValidationEntity jsonPathAPIValidationExpected) {
                    assertInstanceOf(JsonPathAPIValidationEntity.class, validationActual);
                    JsonPathAPIValidationEntity jsonPathAPIValidationActual =
                        (JsonPathAPIValidationEntity) validationActual;
                    assertEquals(jsonPathAPIValidationExpected.getPath(),
                        jsonPathAPIValidationActual.getPath(), "Path must match");
                    assertEquals(jsonPathAPIValidationExpected.getExpectedResponse(),
                        jsonPathAPIValidationActual.getExpectedResponse(), "Expected response must match");
                } else {
                    fail("Unknown validation type: " + validationActual.getClass());
                }
            }
        }

        @Test
        @DisplayName("Information - Name")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateInformationName() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .information(Information.builder()
                    .name("New Name").build())
                .build());
        }

        @Test
        @DisplayName("Information - Description")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateInformationDescription() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .information(Information.builder()
                    .description("New Desc").build())
                .build());

        }

        @Test
        @DisplayName("Information - Tags")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateInformationTags() {
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
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateCronExpression() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .cronExpression("8 7 * * * ?")
                .build());
        }

        @Test
        @DisplayName("Method")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateMethod() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .method(APIMethod.TRACE)
                .build());

        }

        @Test
        @DisplayName("URL")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateUrl() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .url("https://google.com")
                .build());
        }

        @Test
        @DisplayName("Headers")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateHeaders() {
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
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateAuthenticationDefault() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .authenticationDefault(AuthenticationDefaults.NONE)
                .build());
        }

        @Test
        @DisplayName("Payload")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdatePayload() {
            triggerAndValidateUpdate(APIJobPatch.builder()
                .payload("New Payload")
                .build());
        }

        @Test
        @DisplayName("Validations")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdateValidations() {
            List<APIValidation> validationEntities = new ArrayList<>();
            validationEntities.add(new StatusCodeAPIValidation(499));
            validationEntities.add(new JsonPathAPIValidation("$.name", "jerry"));
            validationEntities.add(new MaxResponseTimeAPIValidation(2000));
            triggerAndValidateUpdate(APIJobPatch.builder()
                .validations(validationEntities)
                .build());
        }

        @Test
        @DisplayName("Post Actions")
        @SuppressWarnings({
            "PMD.JUnitTestsShouldIncludeAssert" //False positive done via inheritance
        })
        void positiveUpdatePostActions() {
            List<Action> actionEntities = new ArrayList<>();
            actionEntities.add(new RunJobAction("JOB_1"));
            actionEntities.add(new RunJobAction("JOB_2"));
            actionEntities.add(new RunJobAction("JOB_3"));
            triggerAndValidateUpdate(APIJobPatch.builder()
                .postExecutionActions(actionEntities)
                .build());
        }
    }
}
