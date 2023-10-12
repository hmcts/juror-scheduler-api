package uk.gov.hmcts.juror.scheduler.api.model.bvr;


import uk.gov.hmcts.juror.scheduler.api.model.GenericErrorTest;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.IncorrectPayloadForJobTypeError;

class IncorrectPayloadForJobTypeErrorTest extends GenericErrorTest<IncorrectPayloadForJobTypeError> {
    @Override
    protected String getErrorCode() {
        return "INCORRECT_PAYLOAD_FOR_JOB_TYPE";
    }

    @Override
    protected String getDefaultMessage() {
        return "The payload you have entered is not valid for this type of Job.";
    }

    @Override
    protected IncorrectPayloadForJobTypeError createErrorObject() {
        return new IncorrectPayloadForJobTypeError();
    }
}
