package uk.gov.hmcts.juror.scheduler.datastore.model;

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
