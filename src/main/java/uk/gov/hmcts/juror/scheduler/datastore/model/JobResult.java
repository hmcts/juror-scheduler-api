package uk.gov.hmcts.juror.scheduler.datastore.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class JobResult {

    private boolean passed;

    private ErrorDetails error;

    @Builder
    @Getter
    public static class ErrorDetails {
        private String message;
        private Throwable throwable;
    }
}
