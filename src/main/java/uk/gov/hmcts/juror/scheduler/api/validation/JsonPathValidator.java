package uk.gov.hmcts.juror.scheduler.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JsonPathValidator implements
    ConstraintValidator<JsonPath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }
        try {
            io.restassured.path.json.JsonPath.given("{\"test\":true}").get(value);
        }catch (Exception e){
            return false;
        }
        return true;
    }
}