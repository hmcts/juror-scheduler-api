package uk.gov.hmcts.juror.scheduler.actions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.Set;

@Component
@Slf4j
public class RunJobActionRunner implements ActionRunner {

    private final JobService jobService;

    public RunJobActionRunner(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void trigger(final ActionEntity action, final TaskEntity taskEntity) {
        if (!(action instanceof RunJobActionEntity runJobAction)) {
            throw new InternalServerException("Invalid ActionEntity for this runner: " + action.getClass());
        }
        jobService.executeJob(runJobAction.getJobKey());
    }

    @Override
    public Set<ActionType> supports() {
        return Set.of(ActionType.RUN_JOB);
    }
}
