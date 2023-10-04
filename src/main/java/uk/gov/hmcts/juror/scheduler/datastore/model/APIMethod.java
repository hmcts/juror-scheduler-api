package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;

public enum APIMethod {
    POST,
    GET,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    @JsonCreator
    public static APIMethod forValues(String value) {
        try{
            return valueOf(value);
        }catch (Exception e){
            throw new InvalidEnumValueException("Invalid method entered. Allowed values are: " + Arrays.toString(APIMethod.values()));
        }
    }
}
