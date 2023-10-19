package uk.gov.hmcts.juror.scheduler.api.model.bvr;

import uk.gov.hmcts.juror.scheduler.api.model.GenericErrorTest;
import uk.gov.hmcts.juror.scheduler.api.model.error.bvr.JobAlreadyEnabledError;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class JobAlreadyEnabledErrorTest extends GenericErrorTest<JobAlreadyEnabledError> {
    @Override
    protected String getErrorCode() {
        return "JOB_ALREADY_ENABLED";
    }

    @Override
    protected String getDefaultMessage() {
        return "This Job is already enabled";
    }

    @Override
    protected JobAlreadyEnabledError createErrorObject() {
        return new JobAlreadyEnabledError();
    }
}
