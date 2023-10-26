package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;

@Slf4j
@Getter
public enum ValidationType {
    JSON_PATH,
    STATUS_CODE,
    MAX_RESPONSE_TIME;

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static ValidationType forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException("Invalid validation type entered. Allowed values are: "
                + Arrays.toString(ValidationType.values()));
        }
    }

}
