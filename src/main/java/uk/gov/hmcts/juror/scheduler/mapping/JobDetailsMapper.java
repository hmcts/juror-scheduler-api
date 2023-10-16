package uk.gov.hmcts.juror.scheduler.mapping;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetailsResponse;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
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

    @Mapping(target = "job", ignore = true)
    public abstract StatusCodeValidationEntity toEntity(StatusCodeAPIValidation statusCodeValidation);

    @Mapping(target = "type", ignore = true)
    public abstract StatusCodeAPIValidation toEntity(StatusCodeValidationEntity statusCodeValidation);


    @Mapping(target = "job", ignore = true)
    public abstract MaxResponseTimeAPIValidationEntity toEntity(MaxResponseTimeAPIValidation statusCodeValidation);

    @Mapping(target = "type", ignore = true)
    public abstract MaxResponseTimeAPIValidation toEntity(MaxResponseTimeAPIValidationEntity statusCodeValidation);


    @Mapping(target = "job", ignore = true)
    public abstract JsonPathAPIValidationEntity toEntity(JsonPathAPIValidation statusCodeValidation);

    @Mapping(target = "type", ignore = true)
    public abstract JsonPathAPIValidation toEntity(JsonPathAPIValidationEntity statusCodeValidation);

    @AfterMapping
    public void updateValidationsJobs(@MappingTarget APIJobDetailsEntity apiJobDetailsEntity) {
        if (!CollectionUtils.isEmpty(apiJobDetailsEntity.getValidations())) {
            apiJobDetailsEntity.getValidations().forEach(validation -> validation.setJob(apiJobDetailsEntity));
        }
    }

    public APIValidationEntity apiValidationToEntry(APIValidation apiValidation) {
        //TODO make dynamic
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
        //TODO make dynamic
        if (apiValidation == null) {
            return null;
        }
        if (apiValidation instanceof StatusCodeValidationEntity statusCodeValidation) {
            return toEntity(statusCodeValidation);
        }
        if (apiValidation instanceof MaxResponseTimeAPIValidationEntity maxResponseTimeAPIValidation) {
            return toEntity(maxResponseTimeAPIValidation);
        }
        if (apiValidation instanceof JsonPathAPIValidationEntity jsonPathAPIValidation) {
            return toEntity(jsonPathAPIValidation);
        }
        throw new UnsupportedOperationException("Unknown validation type: " + apiValidation.getClass());
    }

    public List<APIValidation> apiValidationList(List<? extends APIValidationEntity> validationList) {
        if (CollectionUtils.isEmpty(validationList)) {
            return Collections.emptyList();
        }
        return validationList.stream().map(this::apiValidationEntryToValidation).toList();
    }

}
