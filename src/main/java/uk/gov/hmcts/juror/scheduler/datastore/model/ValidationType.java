package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.JsonPathAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.MaxResponseTimeAPIValidation;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.StatusCodeAPIValidation;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;

@Slf4j
@Getter
public enum ValidationType {
    JSON_PATH(JsonPathAPIValidation.class),
    STATUS_CODE(StatusCodeAPIValidation.class),
    MAX_RESPONSE_TIME(MaxResponseTimeAPIValidation.class);

    private final Class<? extends APIValidation> validationClass;

    ValidationType(Class<? extends APIValidation> validationClass) {
        this.validationClass = validationClass;
    }

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static ValidationType forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException("Invalid validation type entered. Allowed values are: " + Arrays.toString(ValidationType.values()));
        }
    }

}
