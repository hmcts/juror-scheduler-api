package uk.gov.hmcts.juror.scheduler.service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;
import uk.gov.hmcts.juror.standard.api.model.error.bvr.BusinessRuleError;
import uk.gov.hmcts.juror.standard.service.exceptions.APIHandleableException;
import uk.gov.hmcts.juror.standard.service.exceptions.BusinessRuleValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusinessRuleValidationExceptionTest {

    @Test
    void positiveConstructorNotAScheduledJob() {
        BusinessRuleError businessRuleError = new NotAScheduledJobError();
        BusinessRuleValidationException exception = new BusinessRuleValidationException(businessRuleError);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode(), "Status code must match");
        assertEquals(businessRuleError, exception.getErrorObject(), "Error object must match");
        assertEquals(APIHandleableException.Type.INFORMATIONAL, exception.getExceptionType(),
            "Exception type must match");
        assertEquals("NOT_A_SCHEDULED_JOB [The action you are trying to perform is only valid on Scheduled Jobs.]",
            exception.getMessage(), "Message must match");
        assertNull(exception.getCause(),"Cause should be null");
    }

    @Test
    void positiveConstructorJobAlreadyDisabled() {
        BusinessRuleError businessRuleError = new JobAlreadyDisabledError();
        BusinessRuleValidationException exception = new BusinessRuleValidationException(businessRuleError);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode(), "Status must match");
        assertEquals(businessRuleError, exception.getErrorObject(), "Exception message must match");
        assertEquals(APIHandleableException.Type.INFORMATIONAL, exception.getExceptionType(),
            "Exception type must match");
        assertEquals("JOB_ALREADY_DISABLED [This Job is already disabled]", exception.getMessage(),
            "Message must match");
        assertNull(exception.getCause(), "Cause must be null");
    }
}
