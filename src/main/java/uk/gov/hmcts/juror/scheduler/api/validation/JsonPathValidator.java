package uk.gov.hmcts.juror.scheduler.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import io.restassured.path.json.JsonPath;
public class JsonPathValidator implements
    ConstraintValidator<uk.gov.hmcts.juror.scheduler.api.validation.JsonPath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }
        try {
            final JsonPath jsonPath = JsonPath.given("{\"test\":true}");
            jsonPath.get(value);
        }catch (Exception e){
            return false;
        }
        return true;
    }
}