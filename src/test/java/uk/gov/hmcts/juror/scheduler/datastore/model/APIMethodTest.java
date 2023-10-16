package uk.gov.hmcts.juror.scheduler.datastore.model;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class APIMethodTest extends EnumTest<APIMethod> {
    @Override
    protected Class<APIMethod> getEnumClass() {
        return APIMethod.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "method";
    }
}
