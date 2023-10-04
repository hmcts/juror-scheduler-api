package uk.gov.hmcts.juror.scheduler.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.ParseException;

public class CronExpressionValidator  implements
    ConstraintValidator<CronExpression, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }
        try{
            new org.quartz.CronExpression(value);
            return true;
        }catch (ParseException e){
            context.disableDefaultConstraintViolation();
            context
                .buildConstraintViolationWithTemplate("Invalid Cron Expression: " + e.getMessage())
                .addConstraintViolation();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
