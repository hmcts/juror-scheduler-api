package uk.gov.hmcts.juror.scheduler.actions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.service.contracts.JobService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class RunJobActionRunnerImplTest {

    private JobService jobService;
    private RunJobActionRunner runJobActionRunner;

    @BeforeEach
    void beforeEach() {
        this.jobService = mock(JobService.class);
        this.runJobActionRunner = new RunJobActionRunner(this.jobService);
    }

    @Test
    void positiveTriggerJobCorrectType() {
        final String expectedJobKey = "JOB_KEY";
        RunJobActionEntity runJobAction = new RunJobActionEntity(expectedJobKey);
        TaskEntity taskEntity = new TaskEntity();
        this.runJobActionRunner.trigger(runJobAction, taskEntity);

        verify(jobService, times(1)).executeJob(expectedJobKey);
        verifyNoMoreInteractions(jobService);
    }

    @Test
    void negativeTriggerJobWrongType() {
        TaskEntity taskEntity = new TaskEntity();
        InternalServerException exception = assertThrows(InternalServerException.class,
            () -> this.runJobActionRunner.trigger(new TestAction(), taskEntity), "Expect exception");
        verifyNoInteractions(jobService);
        assertEquals(
            "Invalid ActionEntity for this runner: "
                + "class uk.gov.hmcts.juror.scheduler.actions.RunJobActionRunnerImplTest$TestAction",
            exception.getMessage(), "Messages must match");
    }

    @SuppressWarnings({
        "PMD.TestClassWithoutTestCases"//False positive - support class
    })
    private static class TestAction extends ActionEntity {

        @Override
        public ActionType getType() {
            return null;
        }

    }
}
