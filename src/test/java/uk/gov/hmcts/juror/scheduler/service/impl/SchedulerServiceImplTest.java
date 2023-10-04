package uk.gov.hmcts.juror.scheduler.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.service.jobs.APIJob;
import uk.gov.hmcts.juror.standard.components.SystemUtil;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SchedulerServiceImpl")
class SchedulerServiceImplTest {


    private Scheduler scheduler;

    private SchedulerServiceImpl schedulerService;
    private static final String jobKey = "ABC123";
    private static final String cronExpression = "* 5 * * * ?";


    @BeforeEach
    public void beforeEach(){
        this.scheduler = mock(Scheduler.class);
        this.schedulerService = new SchedulerServiceImpl(this.scheduler);
    }

    @DisplayName("public void postConstruct()")
    @Nested
    class PostConstruct {
        @Test
        @DisplayName("Scheduler starts")
        void postConstruct() throws SchedulerException {
            schedulerService.postConstruct();
            verify(scheduler, times(1)).start();
        }


        @Test
        @DisplayName("Scheduler starts with exception")
        void postConstruct_exception() throws SchedulerException {
            try (MockedStatic<SystemUtil> systemUtilMockedStatic = Mockito.mockStatic(SystemUtil.class)) {
                doThrow(new RuntimeException()).when(scheduler).start();
                schedulerService.postConstruct();
                verify(scheduler,times(1)).start();
                systemUtilMockedStatic.verify(() -> SystemUtil.exit(1),times(1));
                systemUtilMockedStatic.verifyNoMoreInteractions();
            }
        }
    }

    @DisplayName("public void preDestroy()")
    @Nested
    class PreConstruct {
        @Test
        @DisplayName("Scheduler shutdown")
        void preDestroy() throws SchedulerException {
            schedulerService.preDestroy();
            verify(scheduler, times(1)).shutdown();
        }

        @Test
        @DisplayName("Scheduler shutdown -  Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).shutdown();
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> {
                    schedulerService.preDestroy();
                }
            );
            assertEquals("Failed to shutdown scheduler", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public void register(APIJobDetailsEntity jobDetails)")
    @Nested
    class Register {
        @Test
        @DisplayName("Register scheduled Job")
        void scheduledJob() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(jobKey)
                .cronExpression("* 5 * * * ?")
                .build();

            schedulerService.register(apiJobDetailsEntity);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
            final ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());


            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertEquals(jobKey, jobDetail.getKey().getName());
            assertEquals(APIJob.class, jobDetail.getJobClass());
            assertEquals(1, jobDetail.getJobDataMap().size());
            assertEquals(jobKey, jobDetail.getJobDataMap().getString("key"));

            Trigger trigger = triggerCaptor.getValue();

            assertEquals(jobKey, trigger.getKey().getName());
            assertThat(trigger, instanceOf(CronTriggerImpl.class));
            CronTriggerImpl cronTrigger = (CronTriggerImpl) trigger;
            assertEquals(cronExpression, cronTrigger.getCronExpression());
        }

        @Test
        @DisplayName("Register unscheduled Job")
        void unscheduledJob() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(jobKey)
                .build();

            schedulerService.register(apiJobDetailsEntity);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
            final ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());


            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertEquals(jobKey, jobDetail.getKey().getName());
            assertEquals(APIJob.class, jobDetail.getJobClass());
            assertEquals(1, jobDetail.getJobDataMap().size());
            assertEquals(jobKey, jobDetail.getJobDataMap().getString("key"));

            Trigger trigger = triggerCaptor.getValue();

            assertEquals(jobKey, trigger.getKey().getName());
            assertThat(trigger, instanceOf(SimpleTriggerImpl.class));

        }

        @Test
        @DisplayName("Register Job -  Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(jobKey)
                .build();

            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).scheduleJob(any(), any());


            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.register(apiJobDetailsEntity)
            );
            assertEquals("Failed to register Job", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public void executeJob(String jobKey)")
    @Nested
    class ExecuteJob {
        @Test
        @DisplayName("Execute Job")
        void executeJob() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(new CronTriggerImpl());
            schedulerService.executeJob(jobKey);
            final ArgumentCaptor<JobKey> jobKeyCaptor = ArgumentCaptor.forClass(JobKey.class);

            verify(scheduler, times(1)).triggerJob(jobKeyCaptor.capture());
            assertEquals(jobKey, jobKeyCaptor.getValue().getName());
        }

        @Test
        @DisplayName("Execute Job - Manual")
        void executeJobManual() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(null);
            schedulerService.executeJob(jobKey);
            final ArgumentCaptor<JobDetail> jobDetailsCaptor = ArgumentCaptor.forClass(JobDetail.class);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailsCaptor.capture(), triggerCaptor.capture());
            assertEquals(jobKey, jobDetailsCaptor.getValue().getKey().getName());
            assertEquals(jobKey, triggerCaptor.getValue().getKey().getName());
        }

        @Test
        @DisplayName("Execute Job - Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(new CronTriggerImpl());
            final ArgumentCaptor<JobKey> jobKeyCaptor = ArgumentCaptor.forClass(JobKey.class);
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).triggerJob(any());
            InternalServerException exception = assertThrows(InternalServerException.class, () -> {
                schedulerService.executeJob(jobKey);
            });
            verify(scheduler, times(1)).triggerJob(jobKeyCaptor.capture());
            assertEquals(jobKey, jobKeyCaptor.getValue().getName());
            assertEquals("Failed to execute Job", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public boolean isScheduled(String jobKey)")
    @Nested
    class IsScheduled {
        @Test
        @DisplayName("Is Scheduled")
        void positive_is_scheduled_job() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTrigger(eq(triggerKey))).thenReturn(new CronTriggerImpl());
                assertTrue(schedulerService.isScheduled(jobKey));
            }
        }

        @Test
        @DisplayName("Is not Scheduled")
        void positive_is_not_scheduled_job() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);

                when(scheduler.getTrigger(eq(triggerKey))).thenReturn(new SimpleTriggerImpl());
                assertFalse(schedulerService.isScheduled(jobKey));
            }
        }

        @Test
        @DisplayName("Trigger not found")
        void negative_trigger_not_found() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTrigger(eq(triggerKey))).thenReturn(null);


                NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    assertFalse(schedulerService.isScheduled(jobKey)));
                assertEquals("Trigger from Job Key: " + jobKey + " not found", exception.getMessage());

            }
        }
    }

    @DisplayName("public boolean isEnabled(String jobKey)")
    @Nested
    class IsEnabled {
        @Test
        @DisplayName("Is Enabled")
        void positive_job_is_enabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(eq(triggerKey))).thenReturn(Trigger.TriggerState.NORMAL);
                assertTrue(schedulerService.isEnabled(jobKey));
            }
        }

        @Test
        @DisplayName("Is Disabled")
        void negative_job_is_disabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(eq(triggerKey))).thenReturn(Trigger.TriggerState.PAUSED);
                assertFalse(schedulerService.isEnabled(jobKey));
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).getTriggerState(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.isEnabled(jobKey)
            );
            assertEquals("Failed to get trigger state", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public boolean isDisabled(String jobKey)")
    @Nested
    class IsDisabled {
        @Test
        @DisplayName("Is Enabled")
        void positive_job_is_enabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(eq(triggerKey))).thenReturn(Trigger.TriggerState.NORMAL);
                assertFalse(schedulerService.isDisabled(jobKey));
            }
        }

        @Test
        @DisplayName("Is Disabled")
        void negative_job_is_disabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(jobKey);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(eq(jobKey)))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(eq(triggerKey))).thenReturn(Trigger.TriggerState.PAUSED);
                assertTrue(schedulerService.isDisabled(jobKey));
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).getTriggerState(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.isEnabled(jobKey)
            );
            assertEquals("Failed to get trigger state", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }


    @DisplayName("public void unregister(String jobKey)")
    @Nested
    class UnRegister {
        @Test
        @DisplayName("Successful")
        void positive_unregister_success() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(jobKey);
                jobKeyUtilities.when(() -> JobKey.jobKey(eq(jobKey)))
                    .thenReturn(jobKeyObj);
                schedulerService.unregister(jobKey);
                verify(scheduler, times(1)).deleteJob(eq(jobKeyObj));
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).deleteJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.unregister(jobKey)
            );
            assertEquals("Failed to unregister Job", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public void disable(String jobKey)")
    @Nested
    class Disable {
        @Test
        @DisplayName("Successful")
        void positive_disable_success() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(jobKey);
                jobKeyUtilities.when(() -> JobKey.jobKey(eq(jobKey)))
                    .thenReturn(jobKeyObj);
                schedulerService.disable(jobKey);
                verify(scheduler, times(1)).pauseJob(eq(jobKeyObj));
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).pauseJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.disable(jobKey)
            );
            assertEquals("Failed to disable Job '" + jobKey + "'", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }

    @DisplayName("public void enable(String jobKey)")
    @Nested
    class Enable {
        @Test
        @DisplayName("Successful")
        void positive_enable_success() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(jobKey);
                jobKeyUtilities.when(() -> JobKey.jobKey(eq(jobKey)))
                    .thenReturn(jobKeyObj);
                schedulerService.enable(jobKey);
                verify(scheduler, times(1)).resumeJob(eq(jobKeyObj));
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negative_unexpected_exception() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).resumeJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.enable(jobKey)
            );
            assertEquals("Failed to enable Job '" + jobKey + "'", exception.getMessage());
            assertEquals(thrownException, exception.getCause());
        }
    }
}
