package uk.gov.hmcts.juror.scheduler.datastore.model.filter;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.juror.scheduler.datastore.model.Status;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Getter
public class TaskSearchFilter {

    private String jobKey;
    private Set<Status> statuses;
    private LocalDateTime fromDate;
}
