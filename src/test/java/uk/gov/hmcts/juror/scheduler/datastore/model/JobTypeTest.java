package uk.gov.hmcts.juror.scheduler.datastore.model;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class JobTypeTest extends EnumTest<JobType> {
    @Override
    protected Class<JobType> getEnumClass() {
        return JobType.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "job type";
    }
}
