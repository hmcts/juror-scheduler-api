package uk.gov.hmcts.juror.scheduler.datastore.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;

public enum Status {

    PENDING,
    PROCESSING,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    PROGRESSING,
    FAILED_UNEXPECTED_EXCEPTION,
    SUCCESS,
    FAILED,
    INDETERMINATE;

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static Status forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException(
                "Invalid status entered. Allowed values are: " + Arrays.toString(Status.values()));
        }
    }
}
