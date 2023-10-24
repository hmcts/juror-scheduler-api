package uk.gov.hmcts.juror.scheduler.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.scheduler.actions.ActionRunner;
import uk.gov.hmcts.juror.scheduler.actions.RunJobActionRunner;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.model.ActionType;
import uk.gov.hmcts.juror.scheduler.datastore.model.ConditionType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@DisplayName("ActionServiceImpl")
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals"
})
public class ActionServiceImplTest {


    private RunJobActionEntity runJobActionEntity;

    private ConditionType conditionType;
    private ActionRunner actionRunner;

    private ActionServiceImpl actionService;

    @BeforeEach
    void beforeEach() {
        this.runJobActionEntity = mock(RunJobActionEntity.class);
        this.conditionType = mock(ConditionType.class);
        this.actionRunner = mock(RunJobActionRunner.class);

        when(actionRunner.supports()).thenReturn(Set.of(ActionType.RUN_JOB));
        when(runJobActionEntity.getCondition()).thenReturn(conditionType);
        when(runJobActionEntity.getType()).thenReturn(ActionType.RUN_JOB);

        this.actionService = new ActionServiceImpl(List.of(actionRunner));
    }

    @DisplayName("public TaskEntity taskUpdated(TaskEntity taskEntity)")
    @Nested
    class TaskUpdated {


        @Test
        @DisplayName("Job has no actions")
        void negativeHasNoActions() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setJob(apiJobDetailsEntity);
            assertEquals(taskEntity, actionService.taskUpdated(taskEntity),
                "Returned taskEntity must match inputted value");
            assertNull(taskEntity.getPostActionsMessage(),
                "No Post message should be set");
            verify(actionRunner,times(1)).supports();
            verifyNoMoreInteractions(actionRunner);
        }

        @Test
        @DisplayName("Job actions executed")
        void positiveActionsExecuted() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.addExecutionAction(runJobActionEntity);

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setJob(apiJobDetailsEntity);


            when(conditionType
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity))
                .thenReturn(true);


            assertEquals(taskEntity, actionService.taskUpdated(taskEntity),
                "Returned taskEntity must match inputted value");
            assertNull(taskEntity.getPostActionsMessage(),
                "No Post message should be set");

            verify(runJobActionEntity, times(1)).getCondition();
            verify(conditionType, times(1))
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity);

            verify(actionRunner, times(1))
                .trigger(runJobActionEntity, taskEntity);
        }


        @Test
        @DisplayName("Unexpected exception")
        void negativeUnexpectedException() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.addExecutionAction(runJobActionEntity);

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setJob(apiJobDetailsEntity);


            when(conditionType
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity))
                .thenReturn(true);

            Exception cause = new RuntimeException("I am the cause");

            doThrow(cause).when(actionRunner).trigger(any(), any());


            assertEquals(taskEntity, actionService.taskUpdated(taskEntity),
                "Returned taskEntity must match inputted value");
            assertEquals("Failed to run post actions. Unexpected exception", taskEntity.getPostActionsMessage(),
                "Post message should be set");

            verify(runJobActionEntity, times(1)).getCondition();
            verify(conditionType, times(1))
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity);

            verify(actionRunner, times(1)).trigger(runJobActionEntity, taskEntity);
        }

        @Test
        @DisplayName("Message appends")
        void positiveMessageAppends() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.addExecutionAction(runJobActionEntity);

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setJob(apiJobDetailsEntity);
            taskEntity.appendPostActionsMessage("The start of my message is here");


            when(conditionType
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity))
                .thenReturn(true);

            Exception cause = new RuntimeException("I am the cause");

            doThrow(cause).when(actionRunner).trigger(any(), any());


            assertEquals(taskEntity, actionService.taskUpdated(taskEntity),
                "Returned taskEntity must match inputted value");
            assertEquals("The start of my message is here, Failed to run post actions. Unexpected exception",
                taskEntity.getPostActionsMessage(),
                "Post message should be set");

            verify(runJobActionEntity, times(1)).getCondition();
            verify(conditionType, times(1))
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity);

            verify(actionRunner, times(1)).trigger(runJobActionEntity, taskEntity);

        }

        @DisplayName("No Job actions are met")
        @Test
        void negativeHasNoJobActionsAreMet() {
            APIJobDetailsEntity apiJobDetailsEntity = new APIJobDetailsEntity();
            apiJobDetailsEntity.addExecutionAction(runJobActionEntity);

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setJob(apiJobDetailsEntity);


            when(conditionType
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity))
                .thenReturn(false);


            assertEquals(taskEntity, actionService.taskUpdated(taskEntity),
                "Returned taskEntity must match inputted value");
            assertNull(taskEntity.getPostActionsMessage(),
                "No Post message should be set");

            verify(runJobActionEntity, times(1)).getCondition();
            verify(conditionType, times(1))
                .isMet(ConditionType.TaskEntityChangedListener.class, taskEntity);

            verify(actionRunner, never()).trigger(any(),any());
        }
    }

    @DisplayName("boolean hasActions(APIJobDetailsEntity job)")
    @Nested
    class HasAction {
        @Test
        @DisplayName("Has single action")
        void hasAction() {
            APIJobDetailsEntity job = new APIJobDetailsEntity();
            job.setPostExecutionActions(List.of(
                mock(RunJobActionEntity.class)));
            assertTrue(ActionServiceImpl.hasActions(job),
                "Should return true as actions exist");
        }

        @Test
        @DisplayName("Has multiple action")
        void hasMultipleAction() {
            APIJobDetailsEntity job = new APIJobDetailsEntity();
            job.setPostExecutionActions(List.of(
                mock(RunJobActionEntity.class),
                mock(RunJobActionEntity.class),
                mock(RunJobActionEntity.class)));
            assertTrue(ActionServiceImpl.hasActions(job),
                "Should return true as actions exist");
        }

        @Test
        @DisplayName("Has empty actions")
        void hasEmptyActions() {
            APIJobDetailsEntity job = new APIJobDetailsEntity();
            job.setPostExecutionActions(Collections.emptyList());
            assertFalse(ActionServiceImpl.hasActions(job),
                "Should return false as no actions exist");
        }

        @Test
        @DisplayName("Has null actions")
        void hasNullAction() {
            APIJobDetailsEntity job = new APIJobDetailsEntity();
            job.setPostExecutionActions(null);
            assertFalse(ActionServiceImpl.hasActions(job),
                "Should return false as no actions exist");
        }
    }
}
