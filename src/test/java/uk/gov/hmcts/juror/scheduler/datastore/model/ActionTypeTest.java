package uk.gov.hmcts.juror.scheduler.datastore.model;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases" //False positive done via inheritance
})
class ActionTypeTest extends EnumTest<ActionType> {
    @Override
    protected Class<ActionType> getEnumClass() {
        return ActionType.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "action type";
    }
}
