package uk.gov.hmcts.juror.scheduler.datastore.model;

class StatusTest extends EnumTest<Status>{
    @Override
    protected Class<Status> getEnumClass() {
        return Status.class;
    }

    @Override
    protected String getErrorPrefix() {
        return "status";
    }
}
