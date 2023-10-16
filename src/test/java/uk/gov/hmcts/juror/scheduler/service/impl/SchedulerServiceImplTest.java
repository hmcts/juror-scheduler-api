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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SchedulerServiceImpl")
@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
class SchedulerServiceImplTest {


    private Scheduler scheduler;

    private SchedulerServiceImpl schedulerService;
    private static final String JOB_KEY = "ABC123";
    private static final String CRON_EXPRESSION = "* 5 * * * ?";


    @BeforeEach
    public void beforeEach() {
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
        void postConstructException() throws SchedulerException {
            try (MockedStatic<SystemUtil> systemUtilMockedStatic = Mockito.mockStatic(SystemUtil.class)) {
                doThrow(new RuntimeException()).when(scheduler).start();
                schedulerService.postConstruct();
                verify(scheduler, times(1)).start();
                systemUtilMockedStatic.verify(() -> SystemUtil.exit(1), times(1));
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
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).shutdown();
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> {
                    schedulerService.preDestroy();
                }
            );
            assertEquals("Failed to shutdown scheduler", exception.getMessage(),
                "Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public void register(APIJobDetailsEntity jobDetails)")
    @Nested
    class Register {
        @Test
        @DisplayName("Register scheduled Job")
        void scheduledJob() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(JOB_KEY)
                .cronExpression("* 5 * * * ?")
                .build();

            schedulerService.register(apiJobDetailsEntity);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
            final ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());


            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertEquals(JOB_KEY, jobDetail.getKey().getName(),"Name must match");
            assertEquals(APIJob.class, jobDetail.getJobClass(),"Class must match");
            assertEquals(1, jobDetail.getJobDataMap().size(),"Size must match");
            assertEquals(JOB_KEY, jobDetail.getJobDataMap().getString("key"),"Key must match");

            Trigger trigger = triggerCaptor.getValue();

            assertEquals(JOB_KEY, trigger.getKey().getName(),"Name must match");
            assertThat("Trigger should be instance of cronTriggerImpl",trigger, instanceOf(CronTriggerImpl.class));
            CronTriggerImpl cronTrigger = (CronTriggerImpl) trigger;
            assertEquals(CRON_EXPRESSION, cronTrigger.getCronExpression(),"Cron expression must match");
        }

        @Test
        @DisplayName("Register unscheduled Job")
        void unscheduledJob() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(JOB_KEY)
                .build();

            schedulerService.register(apiJobDetailsEntity);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
            final ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());


            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertEquals(JOB_KEY, jobDetail.getKey().getName(),"Name must match");
            assertEquals(APIJob.class, jobDetail.getJobClass(),"Class must match");
            assertEquals(1, jobDetail.getJobDataMap().size(),"Size must match");
            assertEquals(JOB_KEY, jobDetail.getJobDataMap().getString("key"),"Key must match");

            Trigger trigger = triggerCaptor.getValue();

            assertEquals(JOB_KEY, trigger.getKey().getName(),"Name must match");
            assertThat("Trigger should be instance of simpleTriggerImpl",trigger, instanceOf(SimpleTriggerImpl.class));

        }

        @Test
        @DisplayName("Register Job -  Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            APIJobDetailsEntity apiJobDetailsEntity = APIJobDetailsEntity.builder()
                .key(JOB_KEY)
                .build();

            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).scheduleJob(any(), any());


            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.register(apiJobDetailsEntity)
            );
            assertEquals("Failed to register Job", exception.getMessage(),
                "Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public void executeJob(String jobKey)")
    @Nested
    class ExecuteJob {
        @Test
        @DisplayName("Execute Job")
        void executeJob() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(new CronTriggerImpl());
            schedulerService.executeJob(JOB_KEY);
            final ArgumentCaptor<JobKey> jobKeyCaptor = ArgumentCaptor.forClass(JobKey.class);

            verify(scheduler, times(1)).triggerJob(jobKeyCaptor.capture());
            assertEquals(JOB_KEY, jobKeyCaptor.getValue().getName(),"Key must match");
        }

        @Test
        @DisplayName("Execute Job - Manual")
        void executeJobManual() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(null);
            schedulerService.executeJob(JOB_KEY);
            final ArgumentCaptor<JobDetail> jobDetailsCaptor = ArgumentCaptor.forClass(JobDetail.class);
            final ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

            verify(scheduler, times(1)).scheduleJob(jobDetailsCaptor.capture(), triggerCaptor.capture());
            assertEquals(JOB_KEY, jobDetailsCaptor.getValue().getKey().getName(),"Key must match");
            assertEquals(JOB_KEY, triggerCaptor.getValue().getKey().getName(),"Key must match");
        }

        @Test
        @DisplayName("Execute Job - Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            when(scheduler.getTrigger(any())).thenReturn(new CronTriggerImpl());
            final ArgumentCaptor<JobKey> jobKeyCaptor = ArgumentCaptor.forClass(JobKey.class);
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).triggerJob(any());
            InternalServerException exception = assertThrows(InternalServerException.class, () -> {
                schedulerService.executeJob(JOB_KEY);
            });
            verify(scheduler, times(1)).triggerJob(jobKeyCaptor.capture());
            assertEquals(JOB_KEY, jobKeyCaptor.getValue().getName(),"Name must match");
            assertEquals("Failed to execute Job", exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public boolean isScheduled(String jobKey)")
    @Nested
    class IsScheduled {
        @Test
        @DisplayName("Is Scheduled")
        void positiveIsScheduledJob() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTrigger(triggerKey)).thenReturn(new CronTriggerImpl());
                assertTrue(schedulerService.isScheduled(JOB_KEY),"Job should be scheduled");
            }
        }

        @Test
        @DisplayName("Is not Scheduled")
        void positiveIsNotScheduledJob() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);

                when(scheduler.getTrigger(triggerKey)).thenReturn(new SimpleTriggerImpl());
                assertFalse(schedulerService.isScheduled(JOB_KEY),"Job should not be should");
            }
        }

        @Test
        @DisplayName("Trigger not found")
        void negativeTriggerNotFound() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTrigger(triggerKey)).thenReturn(null);


                NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    assertFalse(schedulerService.isScheduled(JOB_KEY),"Job should not be scheduled"));
                assertEquals("Trigger from Job Key: " + JOB_KEY + " not found",
                    exception.getMessage(),"Message must match");
            }
        }
    }

    @DisplayName("public boolean isEnabled(String jobKey)")
    @Nested
    class IsEnabled {
        @Test
        @DisplayName("Is Enabled")
        void positiveJobIsEnabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);
                assertTrue(schedulerService.isEnabled(JOB_KEY),"Job should be enabled");
            }
        }

        @Test
        @DisplayName("Is Disabled")
        void negativeJobIsDisabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.PAUSED);
                assertFalse(schedulerService.isEnabled(JOB_KEY),"Job should not be enabled");
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).getTriggerState(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.isEnabled(JOB_KEY)
            );
            assertEquals("Failed to get trigger state",
                exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public boolean isDisabled(String jobKey)")
    @Nested
    class IsDisabled {
        @Test
        @DisplayName("Is Enabled")
        void positiveJobIsEnabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);
                assertFalse(schedulerService.isDisabled(JOB_KEY),"Job should not be disabled");
            }
        }

        @Test
        @DisplayName("Is Disabled")
        void negativeJobIsDisabled() throws SchedulerException {
            try (MockedStatic<TriggerKey> TriggerKeyUtilities = Mockito.mockStatic(TriggerKey.class)) {
                TriggerKey triggerKey = new TriggerKey(JOB_KEY);
                TriggerKeyUtilities.when(() -> TriggerKey.triggerKey(JOB_KEY))
                    .thenReturn(triggerKey);
                when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.PAUSED);
                assertTrue(schedulerService.isDisabled(JOB_KEY),"Job should be disabled");
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).getTriggerState(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.isEnabled(JOB_KEY)
            );
            assertEquals("Failed to get trigger state",
                exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }


    @DisplayName("public void unregister(String jobKey)")
    @Nested
    class UnRegister {
        @Test
        @DisplayName("Successful")
        void positiveUnregisterSuccess() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(JOB_KEY);
                jobKeyUtilities.when(() -> JobKey.jobKey(JOB_KEY))
                    .thenReturn(jobKeyObj);
                schedulerService.unregister(JOB_KEY);
                verify(scheduler, times(1)).deleteJob(jobKeyObj);
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).deleteJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.unregister(JOB_KEY)
            );
            assertEquals("Failed to unregister Job",
                exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public void disable(String jobKey)")
    @Nested
    class Disable {
        @Test
        @DisplayName("Successful")
        void positiveDisableSuccess() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(JOB_KEY);
                jobKeyUtilities.when(() -> JobKey.jobKey(JOB_KEY))
                    .thenReturn(jobKeyObj);
                schedulerService.disable(JOB_KEY);
                verify(scheduler, times(1)).pauseJob(jobKeyObj);
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).pauseJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.disable(JOB_KEY)
            );
            assertEquals("Failed to disable Job '" + JOB_KEY + "'",
                exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }

    @DisplayName("public void enable(String jobKey)")
    @Nested
    class Enable {
        @Test
        @DisplayName("Successful")
        void positiveEnableSuccess() throws SchedulerException {
            try (MockedStatic<JobKey> jobKeyUtilities = Mockito.mockStatic(JobKey.class)) {
                JobKey jobKeyObj = new JobKey(JOB_KEY);
                jobKeyUtilities.when(() -> JobKey.jobKey(JOB_KEY))
                    .thenReturn(jobKeyObj);
                schedulerService.enable(JOB_KEY);
                verify(scheduler, times(1)).resumeJob(jobKeyObj);
            }
        }

        @Test
        @DisplayName("Unexpected Exception")
        void negativeUnexpectedException() throws SchedulerException {
            Exception thrownException = new RuntimeException("Some Reason");
            doThrow(thrownException).when(scheduler).resumeJob(any());
            InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> schedulerService.enable(JOB_KEY)
            );
            assertEquals("Failed to enable Job '" + JOB_KEY + "'",
                exception.getMessage(),"Message must match");
            assertEquals(thrownException, exception.getCause(),"Cause must match");
        }
    }
}
