package uk.gov.hmcts.juror.scheduler.api.model.bvr;

import uk.gov.hmcts.juror.scheduler.api.model.GenericErrorTest;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyDisabledError;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class JobAlreadyDisabledErrorTest extends GenericErrorTest<JobAlreadyDisabledError> {
    @Override
    protected String getErrorCode() {
        return "JOB_ALREADY_DISABLED";
    }

    @Override
    protected String getDefaultMessage() {
        return "This Job is already disabled";
    }

    @Override
    protected JobAlreadyDisabledError createErrorObject() {
        return new JobAlreadyDisabledError();
    }
}
