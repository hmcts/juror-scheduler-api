package uk.gov.hmcts.juror.scheduler.datastore.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.quartz.Job;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.JobDetails;
import uk.gov.hmcts.juror.scheduler.api.model.job.details.api.APIJobDetails;
import uk.gov.hmcts.juror.scheduler.service.jobs.APIJob;
import uk.gov.hmcts.juror.standard.service.exceptions.InvalidEnumValueException;

import java.util.Arrays;

@Getter
public enum JobType {
    API(APIJobDetails.class, APIJob.class);

    private final Class<? extends JobDetails> jobDetailsClass;

    private final Class<? extends Job> jobProcessingClass;

    JobType(Class<? extends JobDetails> jobDetailsClass, Class<? extends Job> jobProcessingClass) {
        this.jobDetailsClass = jobDetailsClass;
        this.jobProcessingClass = jobProcessingClass;
    }

    @JsonCreator
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static JobType forValues(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            throw new InvalidEnumValueException(
                "Invalid job type entered. Allowed values are: " + Arrays.toString(JobType.values()));
        }
    }
}
