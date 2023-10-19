package uk.gov.hmcts.juror.scheduler.datastore.model;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class ValidationTypeTest extends EnumTest<ValidationType> {
    @Override
    protected Class<ValidationType> getEnumClass() {
        return ValidationType.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "validation type";
    }
}
