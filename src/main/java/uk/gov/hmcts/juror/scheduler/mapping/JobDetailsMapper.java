package uk.gov.hmcts.juror.scheduler.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.Action;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.actions.RunJobAction;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
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

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.AvoidDuplicateLiterals"
})
//TODO test
public abstract class JobDetailsMapper {

    @Mapping(target = "lastUpdatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "tags", source = "information.tags")
    @Mapping(target = "name", source = "information.name")
    @Mapping(target = "description", source = "information.description")
    public abstract APIJobDetailsEntity toAPIJobDetailsEntity(APIJobDetails apiJobDetails);


    @Mapping(target = "information.name", source = "name")
    @Mapping(target = "information.tags", source = "tags")
    @Mapping(target = "information.description", source = "description")
    @Mapping(target = "type", constant = "API")
    public abstract APIJobDetails toJobDetailsJobDetails(APIJobDetailsEntity jobDetails);

    @Mapping(target = "information.name", source = "name")
    @Mapping(target = "information.tags", source = "tags")
    @Mapping(target = "information.description", source = "description")
    @Mapping(target = "type", constant = "API")
    public abstract APIJobDetailsResponse toAPIJobDetailsResponse(APIJobDetailsEntity jobDetails);


    public abstract List<APIJobDetailsResponse> toJobDetailsJobDetailsList(List<APIJobDetailsEntity> jobs);


    //Validations

    @Mapping(target = "job", ignore = true)
    public abstract StatusCodeValidationEntity toEntity(StatusCodeAPIValidation statusCodeValidation);

    @Mapping(target = "job", ignore = true)
    public abstract MaxResponseTimeAPIValidationEntity toEntity(MaxResponseTimeAPIValidation statusCodeValidation);

    @Mapping(target = "job", ignore = true)
    public abstract JsonPathAPIValidationEntity toEntity(JsonPathAPIValidation statusCodeValidation);

    @Mapping(target = "job", ignore = true)
    public abstract RunJobActionEntity toEntity(RunJobAction runJobAction);

    @Mapping(target = "type", ignore = true)
    public abstract StatusCodeAPIValidation fromEntity(StatusCodeValidationEntity statusCodeValidation);

    @Mapping(target = "type", ignore = true)
    public abstract MaxResponseTimeAPIValidation fromEntity(MaxResponseTimeAPIValidationEntity statusCodeValidation);


    @Mapping(target = "type", ignore = true)
    public abstract JsonPathAPIValidation fromEntity(JsonPathAPIValidationEntity statusCodeValidation);

    @Mapping(target = "type", ignore = true)
    public abstract RunJobAction fromEntity(RunJobActionEntity runJobActionEntity);

    @AfterMapping
    public void assignJobs(@MappingTarget APIJobDetailsEntity apiJobDetailsEntity) {
        if (!CollectionUtils.isEmpty(apiJobDetailsEntity.getValidations())) {
            apiJobDetailsEntity.getValidations().forEach(validation -> validation.setJob(apiJobDetailsEntity));
            apiJobDetailsEntity.getPostExecutionActions().forEach(action -> action.setJob(apiJobDetailsEntity));
        }
    }

    public APIValidationEntity apiValidationToEntry(APIValidation apiValidation) {
        if (apiValidation == null) {
            return null;
        }
        if (apiValidation instanceof StatusCodeAPIValidation statusCodeValidation) {
            return toEntity(statusCodeValidation);
        }
        if (apiValidation instanceof MaxResponseTimeAPIValidation maxResponseTimeAPIValidation) {
            return toEntity(maxResponseTimeAPIValidation);
        }
        if (apiValidation instanceof JsonPathAPIValidation jsonPathAPIValidation) {
            return toEntity(jsonPathAPIValidation);
        }
        throw new UnsupportedOperationException("Unknown validation type: " + apiValidation.getClass());
    }

    public List<APIValidationEntity> apiValidationEntityList(List<? extends APIValidation> validationList) {
        if (CollectionUtils.isEmpty(validationList)) {
            return Collections.emptyList();
        }
        return validationList.stream().map(this::apiValidationToEntry).toList();
    }

    public APIValidation apiValidationEntryToValidation(APIValidationEntity apiValidation) {
        if (apiValidation == null) {
            return null;
        }
        if (apiValidation instanceof StatusCodeValidationEntity statusCodeValidation) {
            return fromEntity(statusCodeValidation);
        }
        if (apiValidation instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidation) {
            return fromEntity(maxResponseTimeAPIValidation);
        }
        if (apiValidation instanceof JsonPathAPIValidationEntity jsonPathAPIValidation) {
            return fromEntity(jsonPathAPIValidation);
        }
        throw new UnsupportedOperationException("Unknown validation type: " + apiValidation.getClass());
    }

    public List<APIValidation> apiValidationList(List<? extends APIValidationEntity> validationList) {
        if (CollectionUtils.isEmpty(validationList)) {
            return Collections.emptyList();
        }
        return validationList.stream().map(this::apiValidationEntryToValidation).toList();
    }

    public ActionEntity actionToEntry(Action action) {
        if (action == null) {
            return null;
        }
        if (action instanceof RunJobAction runJobAction) {
            return toEntity(runJobAction);
        }
        throw new UnsupportedOperationException("Unknown action type: " + action.getClass());
    }

    public List<ActionEntity> actionsList(List<? extends Action> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return Collections.emptyList();
        }
        return actions.stream().map(this::actionToEntry).toList();
    }

    public Action actionEntryToAction(ActionEntity actionEntity) {
        if (actionEntity == null) {
            return null;
        }
        if (actionEntity instanceof RunJobActionEntity runJobActionEntity) {
            return fromEntity(runJobActionEntity);
        }
        throw new UnsupportedOperationException("Unknown Action type: " + actionEntity.getClass());
    }

    public List<Action> actionList(List<? extends ActionEntity> actionEntities) {
        if (CollectionUtils.isEmpty(actionEntities)) {
            return Collections.emptyList();
        }
        return actionEntities.stream().map(this::actionEntryToAction).toList();
    }
}
