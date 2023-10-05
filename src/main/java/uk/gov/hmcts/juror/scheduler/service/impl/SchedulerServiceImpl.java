package uk.gov.hmcts.juror.scheduler.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.scheduler.datastore.model.JobType;
import uk.gov.hmcts.juror.scheduler.service.contracts.SchedulerService;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.standard.components.SystemUtil;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.gov.hmcts.juror.standard.service.exceptions.NotFoundException;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SchedulerServiceImpl implements SchedulerService {
    private final Scheduler scheduler;

    @Autowired

    protected SchedulerServiceImpl(Scheduler scheduler) {
        this.scheduler = scheduler;
    }


    @PostConstruct
    public void postConstruct() {
        try {
            scheduler.start();
        } catch (Exception exception) {
            log.error("Failed to start the scheduler shutting down.", exception);
            SystemUtil.exit(1);//No point continuing if the scheduler fails to start
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            scheduler.shutdown();
        } catch (Exception exception) {
            log.error("Failed to shutdown scheduler", exception);
            throw new InternalServerException("Failed to shutdown scheduler", exception);
        }
    }

    @Override
    public void register(APIJobDetailsEntity jobDetails) {
        if (jobDetails.getCronExpression() != null) {
            scheduleCronJob(jobDetails);
        } else {
            scheduleManualJob(jobDetails.getKey());
        }
    }

    private void scheduleCronJob(APIJobDetailsEntity jobDetails) {
        final JobDetail jobDetail = buildJobDetails(jobDetails.getKey());
        final Trigger trigger = cronTriggerBuilder(jobDetails);
        scheduleJob(jobDetail, trigger);
    }

    private void scheduleManualJob(String jobKey) {
        final JobDetail jobDetail = buildJobDetails(jobKey);
        final Trigger trigger = simpleTrigger(jobKey);
        scheduleJob(jobDetail, trigger);
    }

    private void scheduleJob(JobDetail jobDetail, Trigger trigger) {
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception exception) {
            log.error("Failed to schedule Job", exception);
            throw new InternalServerException("Failed to register Job", exception);
        }
    }

    @Override
    public void executeJob(String jobKey) {
        try {
            if (getTrigger(jobKey) != null) {
                scheduler.triggerJob(createJobKey(jobKey));
            } else {
                scheduleManualJob(jobKey);
            }
        } catch (Exception exception) {
            log.error("Failed to execute Job", exception);
            throw new InternalServerException("Failed to execute Job", exception);
        }
    }

    @Override
    @SneakyThrows
    public boolean isScheduled(String jobKey) {
        Trigger trigger = getTrigger(jobKey);
        if (trigger == null) {
            throw new NotFoundException("Trigger from Job Key: " + jobKey + " not found");
        }

        return trigger instanceof CronTrigger;
    }

    private Trigger getTrigger(String jobKey) throws SchedulerException {
        return scheduler.getTrigger(TriggerKey.triggerKey(jobKey));
    }

    @Override
    public boolean isEnabled(String jobKey) {
        return !isDisabled(jobKey);
    }

    @Override
    public boolean isDisabled(String jobKey) {
        try {
            return scheduler.getTriggerState(TriggerKey.triggerKey(jobKey)) == Trigger.TriggerState.PAUSED;
        } catch (Exception e) {
            log.error("Failed to get trigger state", e);
            throw new InternalServerException("Failed to get trigger state", e);
        }
    }

    @Override
    public void unregister(String jobKey) {
        try {
            scheduler.deleteJob(createJobKey(jobKey));
        } catch (Exception exception) {
            log.error("Failed to unregister Job", exception);
            throw new InternalServerException("Failed to unregister Job", exception);
        }
    }

    @Override
    public void disable(String jobKey) {
        try {
            scheduler.pauseJob(createJobKey(jobKey));
            log.info("Job '" + jobKey + "' successfully disabled");
        } catch (Exception exception) {
            log.error("Failed to disable Job '" + jobKey + "'", exception);
            throw new InternalServerException("Failed to disable Job '" + jobKey + "'", exception);
        }
    }

    @Override
    public void enable(String jobKey) {
        try {
            scheduler.resumeJob(createJobKey(jobKey));
            log.info("Job '" + jobKey + "' successfully enabled");
        } catch (Exception exception) {
            log.error("Failed to enable Job '" + jobKey + "'", exception);
            throw new InternalServerException("Failed to enable Job '" + jobKey + "'", exception);
        }
    }

    private JobKey createJobKey(String jobKey) {
        return JobKey.jobKey(jobKey);
    }


    @SuppressWarnings("PMD.LawOfDemeter")
    private JobDetail buildJobDetails(String jobKey) {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("key", jobKey);
        return JobBuilder
                .newJob(JobType.API.getJobProcessingClass())
                .withIdentity(jobKey)
                .setJobData(jobDataMap)
                .build();
    }

    private Trigger cronTriggerBuilder(APIJobDetailsEntity jobDetails) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobDetails.getKey())
                .withSchedule(CronScheduleBuilder.cronSchedule(jobDetails.getCronExpression()))
                .startNow()
                .build();
    }

    private Trigger simpleTrigger(String jobKey) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobKey)
                .build();
    }
}
