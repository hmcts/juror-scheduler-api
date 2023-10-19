package uk.gov.hmcts.juror.scheduler.datastore.model;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class StatusTest extends EnumTest<Status> {
    @Override
    protected Class<Status> getEnumClass() {
        return Status.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "status";
    }
}
