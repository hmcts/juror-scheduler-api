package uk.gov.hmcts.juror.scheduler.testsupport.util;

import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.Action;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.task.TaskDetail;
import uk.gov.hmcts.juror.scheduler.datastore.entity.TaskEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert",//False positive support library
    "PMD.TooManyMethods",
    "PMD.LinguisticNaming",
    "PMD.AvoidDuplicateLiterals"
})
public final class ValidateUtil {

    private ValidateUtil() {

    }


    private static void validate(Action action, ActionEntity entity) {
        assertEquals(action.getType(), entity.getType(), "Type must match");
        assertEquals(action.getCondition(), entity.getCondition(), "Condition must match");
    }

    private static void validate(ActionEntity entity, Action action) {
        assertEquals(entity.getType(), action.getType(), "Type must match");
        assertEquals(entity.getCondition(), action.getCondition(), "Condition must match");
    }

    private static void validate(APIValidation validation, APIValidationEntity entity) {
        assertEquals(validation.getType(), entity.getType(), "Type must match");
    }

    private static void validate(APIValidationEntity entity, APIValidation validation) {
        assertEquals(entity.getType(), validation.getType(), "Type must match");
    }


    public static void validate(StatusCodeAPIValidation validation, APIValidationEntity entity) {
        validate((APIValidation) validation, entity);
        assertInstanceOf(StatusCodeValidationEntity.class, entity,
            "Entity must be StatusCodeValidationEntity");
        StatusCodeValidationEntity validationEntity = (StatusCodeValidationEntity) entity;
        assertEquals(validation.getExpectedStatusCode(), validationEntity.getExpectedStatusCode(),
            "Status code must match");

    }

    public static void validate(MaxResponseTimeAPIValidation validation, APIValidationEntity entity) {
        validate((APIValidation) validation, entity);
        assertInstanceOf(MaxResponseTimeAPIValidationEntity.class, entity,
            "Entity must be MaxResponseTimeAPIValidationEntity");
        MaxResponseTimeAPIValidationEntity validationEntity = (MaxResponseTimeAPIValidationEntity) entity;
        assertEquals(validation.getMaxResponseTimeMS(), validationEntity.getMaxResponseTimeMS(),
            "Max response time must match");
    }

    public static void validate(JsonPathAPIValidation validation, APIValidationEntity entity) {
        validate((APIValidation) validation, entity);
        assertInstanceOf(JsonPathAPIValidationEntity.class, entity,
            "Entity must be JsonPathAPIValidationEntity");
        JsonPathAPIValidationEntity validationEntity = (JsonPathAPIValidationEntity) entity;
        assertEquals(validation.getPath(), validationEntity.getPath(),
            "Path time must match");
        assertEquals(validation.getExpectedResponse(), validationEntity.getExpectedResponse(),
            "Expected response must match");
    }

    public static void validate(RunJobAction action, ActionEntity entity) {
        validate((Action) action, entity);
        assertInstanceOf(RunJobActionEntity.class, entity,
            "Entity must be RunJobActionEntity");
        RunJobActionEntity actionEntity = (RunJobActionEntity) entity;
        assertEquals(action.getJobKey(), actionEntity.getJobKey(),
            "Job Key must match");
    }

    public static void validate(StatusCodeValidationEntity validationEntity,
                                APIValidation apiValidation) {
        validate((APIValidationEntity) validationEntity, apiValidation);
        assertInstanceOf(StatusCodeAPIValidation.class, apiValidation,
            "Entity must be StatusCodeAPIValidation");
        StatusCodeAPIValidation validation = (StatusCodeAPIValidation) apiValidation;

        assertEquals(validationEntity.getExpectedStatusCode(), validation.getExpectedStatusCode(),
            "Status code must match");

    }

    public static void validate(MaxResponseTimeAPIValidationEntity validationEntity,
                                APIValidation apiValidation) {
        validate((APIValidationEntity) validationEntity, apiValidation);
        assertInstanceOf(MaxResponseTimeAPIValidation.class, apiValidation,
            "Entity must be MaxResponseTimeAPIValidation");
        MaxResponseTimeAPIValidation validation = (MaxResponseTimeAPIValidation) apiValidation;

        assertEquals(validationEntity.getMaxResponseTimeMS(), validation.getMaxResponseTimeMS(),
            "Max response time must match");
    }

    public static void validate(JsonPathAPIValidationEntity validationEntity,
                                APIValidation apiValidation) {
        validate((APIValidationEntity) validationEntity, apiValidation);
        assertInstanceOf(JsonPathAPIValidation.class, apiValidation,
            "Entity must be JsonPathAPIValidation");
        JsonPathAPIValidation validation = (JsonPathAPIValidation) apiValidation;

        assertEquals(validationEntity.getPath(), validation.getPath(),
            "Path time must match");
        assertEquals(validationEntity.getExpectedResponse(), validation.getExpectedResponse(),
            "Expected response must match");
    }

    public static void validate(RunJobActionEntity actionEntity, Action action) {
        validate((ActionEntity) actionEntity, action);
        assertInstanceOf(RunJobAction.class, action,
            "Entity must be RunJobAction");
        RunJobAction actionVal = (RunJobAction) action;
        assertEquals(actionEntity.getJobKey(), actionVal.getJobKey(),
            "Job Key must match");
    }

    public static void validate(TaskEntity taskEntity, TaskDetail task) {
        assertEquals(taskEntity.getJob().getKey(), task.getJobKey(),
            "Job key should match");
        assertEquals(taskEntity.getTaskId(), task.getTaskId(),
            "Task id should match");
        assertEquals(taskEntity.getCreatedAt(), task.getCreatedAt(),
            "createdAt should match");
        assertEquals(taskEntity.getLastUpdatedAt(), task.getLastUpdatedAt(),
            "lastUpdatedAt should match");

        assertEquals(taskEntity.getStatus(), task.getStatus(),
            "Status should match");
        assertEquals(taskEntity.getMessage(), task.getMessage(),
            "Message should match");
        assertEquals(taskEntity.getPostActionsMessage(), task.getPostActionsMessage(),
            "Post Actions message should match");
        assertEquals(taskEntity.getMetaData(), task.getMetaData(),
            "Meta Data should match");
    }

    public static void validateTaskEntityList(List<TaskEntity> taskEntities, List<TaskDetail> tasks) {
        assertEquals(taskEntities.size(), tasks.size(), "Size must match");
        for (int index = 0; index < taskEntities.size(); index++) {
            validate(taskEntities.get(index), tasks.get(index));
        }
    }

    public static void validateAPIValidationEntityList(List<APIValidationEntity> validationEntities,
                                                       List<APIValidation> apiValidations) {
        assertEquals(validationEntities.size(), apiValidations.size(), "Size must match");
        for (int index = 0; index < validationEntities.size(); index++) {
            APIValidationEntity entity = validationEntities.get(index);
            if (entity instanceof StatusCodeValidationEntity) {
                validate((StatusCodeValidationEntity) entity, apiValidations.get(index));
            } else if (entity instanceof MaxResponseTimeAPIValidationEntity) {
                validate((MaxResponseTimeAPIValidationEntity) entity, apiValidations.get(index));
            } else if (entity instanceof JsonPathAPIValidationEntity) {
                validate((JsonPathAPIValidationEntity) entity, apiValidations.get(index));
            } else {
                fail("Unknown entity type; " + entity.getClass());
            }
        }
    }

    public static void validateAPIValidationList(List<APIValidation> apiValidations,
                                                 List<APIValidationEntity> validationEntities) {
        assertEquals(apiValidations.size(), validationEntities.size(), "Size must match");
        for (int index = 0; index < apiValidations.size(); index++) {
            APIValidation validation = apiValidations.get(index);
            if (validation instanceof StatusCodeAPIValidation) {
                validate((StatusCodeAPIValidation) validation, validationEntities.get(index));
            } else if (validation instanceof MaxResponseTimeAPIValidation) {
                validate((MaxResponseTimeAPIValidation) validation, validationEntities.get(index));
            } else if (validation instanceof JsonPathAPIValidation) {
                validate((JsonPathAPIValidation) validation, validationEntities.get(index));
            } else {
                fail("Unknown validation type; " + validation.getClass());
            }
        }
    }

    public static void validateActionEntityList(List<ActionEntity> actionEntityList, List<Action> actions) {
        assertEquals(actionEntityList.size(), actions.size(), "Size must match");
        for (int index = 0; index < actionEntityList.size(); index++) {
            ActionEntity entity = actionEntityList.get(index);
            if (entity instanceof RunJobActionEntity) {
                validate((RunJobActionEntity) entity, actions.get(index));
            } else {
                fail("Unknown entity type; " + entity.getClass());
            }
        }
    }

    public static void validateActionList(List<Action> actions, List<ActionEntity> actionEntities) {
        assertEquals(actions.size(), actionEntities.size(), "Size must match");
        for (int index = 0; index < actions.size(); index++) {
            Action action = actions.get(index);
            if (action instanceof RunJobAction) {
                validate((RunJobAction) action, actionEntities.get(index));
            } else {
                fail("Unknown entity type; " + action.getClass());
            }
        }
    }
}
