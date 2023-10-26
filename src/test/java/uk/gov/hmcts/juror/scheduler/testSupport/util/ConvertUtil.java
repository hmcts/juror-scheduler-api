package uk.gov.hmcts.juror.scheduler.testsupport.util;

import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.Action;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({
    "PMD.AvoidThrowingRawExceptionTypes"
})
public final class ConvertUtil {

    private ConvertUtil() {

    }

    public static List<APIValidationEntity> convertValidations(List<? extends APIValidation> validations) {
        List<APIValidationEntity> list = new ArrayList<>();
        for (APIValidation validation : validations) {
            list.add(convertValidation(validation));
        }
        return list;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    public static APIValidationEntity convertValidation(APIValidation validation) {
        if (validation instanceof StatusCodeAPIValidation statusCodeAPIValidation) {
            return new StatusCodeValidationEntity(statusCodeAPIValidation.getExpectedStatusCode());
        }
        if (validation instanceof MaxResponseTimeAPIValidation maxResponseTimeAPIValidation) {
            return new MaxResponseTimeAPIValidationEntity(maxResponseTimeAPIValidation.getMaxResponseTimeMS());
        }
        if (validation instanceof JsonPathAPIValidation jsonPathAPIValidation) {
            return new JsonPathAPIValidationEntity(jsonPathAPIValidation.getPath(),
                jsonPathAPIValidation.getExpectedResponse());
        }
        throw new RuntimeException("Unknown validation type: " + validation.getClass());
    }

    public static List<ActionEntity> convertPostActions(List<? extends Action> postExecutionActions) {
        List<ActionEntity> list = new ArrayList<>();
        for (Action action : postExecutionActions) {
            list.add(convertPostAction(action));
        }
        return list;
    }

    private static ActionEntity convertPostAction(Action action) {
        if (action instanceof RunJobAction runJobAction) {
            return new RunJobActionEntity(runJobAction.getJobKey());
        }
        throw new RuntimeException("Unknown action type: " + action.getClass());
    }
}
