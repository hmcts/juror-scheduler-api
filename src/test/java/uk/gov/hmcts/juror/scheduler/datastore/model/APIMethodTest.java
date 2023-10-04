package uk.gov.hmcts.juror.scheduler.datastore.model;

class APIMethodTest extends EnumTest<APIMethod>{
    @Override
    protected Class<APIMethod> getEnumClass() {
        return APIMethod.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "method";
    }
}
