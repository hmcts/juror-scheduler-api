package uk.gov.hmcts.juror.scheduler.testsupport.util;

import uk.gov.hmcts.juror.scheduler.datastore.entity.action.ActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.action.RunJobActionEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIJobDetailsEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.APIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.JsonPathAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.MaxResponseTimeAPIValidationEntity;
import uk.gov.hmcts.juror.scheduler.datastore.entity.api.StatusCodeValidationEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({
    "PMD.AvoidThrowingRawExceptionTypes"
})
public final class CloneUtil {

    private CloneUtil() {

    }

    public static APIJobDetailsEntity cloneAPIJobDetailsEntity(APIJobDetailsEntity baseApiJobDetailsEntity) {
        return APIJobDetailsEntity.builder()
            .key(baseApiJobDetailsEntity.getKey())
            .cronExpression(baseApiJobDetailsEntity.getCronExpression())
            .method(baseApiJobDetailsEntity.getMethod())
            .name(baseApiJobDetailsEntity.getName())
            .description(baseApiJobDetailsEntity.getDescription())
            .tags(cloneSet(baseApiJobDetailsEntity.getTags()))
            .url(baseApiJobDetailsEntity.getUrl())
            .payload(baseApiJobDetailsEntity.getPayload())
            .authenticationDefault(baseApiJobDetailsEntity.getAuthenticationDefault())
            .createdAt(baseApiJobDetailsEntity.getCreatedAt())
            .lastUpdatedAt(baseApiJobDetailsEntity.getLastUpdatedAt())
            .headers(cloneMap(baseApiJobDetailsEntity.getHeaders()))
            .validations(cloneValidations(baseApiJobDetailsEntity.getValidations()))
            .postExecutionActions(clonePostExecutionActions(baseApiJobDetailsEntity.getPostExecutionActions()))
            .build();
    }


    private static List<ActionEntity> clonePostExecutionActions(List<ActionEntity> postExecutionActions) {
        List<ActionEntity> list = new ArrayList<>();
        for (ActionEntity action : postExecutionActions) {
            list.add(cloneActionEntity(action));
        }
        return list;
    }

    private static ActionEntity cloneActionEntity(ActionEntity action) {
        if (action instanceof RunJobActionEntity runJobAction) {
            return new RunJobActionEntity(runJobAction.getJobKey());
        }
        throw new RuntimeException("Unknown actionEntity type: " + action.getClass());
    }

    private static List<APIValidationEntity> cloneValidations(List<APIValidationEntity> validations) {
        List<APIValidationEntity> list = new ArrayList<>();
        for (APIValidationEntity validation : validations) {
            list.add(cloneValidation(validation));
        }
        return list;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static APIValidationEntity cloneValidation(APIValidationEntity validation) {
        if (validation instanceof StatusCodeValidationEntity statusCodeValidation) {
            return new StatusCodeValidationEntity(statusCodeValidation.getExpectedStatusCode());
        }
        if (validation instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidation) {
            return new StatusCodeValidationEntity(maxResponseTimeAPIValidation.getMaxResponseTimeMS());
        }
        if (validation instanceof JsonPathAPIValidationEntity jsonPathAPIValidation) {
            return new JsonPathAPIValidationEntity(jsonPathAPIValidation.getPath(),
                jsonPathAPIValidation.getExpectedResponse());
        }
        throw new RuntimeException("Unknown validation type: " + validation.getClass());
    }

    private static Map<String, String> cloneMap(Map<String, String> headers) {
        return new HashMap<>(headers);
    }

    private static Set<String> cloneSet(Set<String> tags) {
        return new HashSet<>(tags);
    }

}
