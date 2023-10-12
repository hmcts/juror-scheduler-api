package uk.gov.hmcts.juror.scheduler.api.model.bvr;

import uk.gov.hmcts.juror.scheduler.api.model.GenericErrorTest;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.NotAScheduledJobError;

class NotAScheduledJobErrorTest extends GenericErrorTest<NotAScheduledJobError> {
    @Override
    protected String getErrorCode() {
        return "NOT_A_SCHEDULED_JOB";
    }

    @Override
    protected String getDefaultMessage() {
        return "The action you are trying to perform is only valid on Scheduled Jobs.";
    }

    @Override
    protected NotAScheduledJobError createErrorObject() {
        return new NotAScheduledJobError();
    }
}
