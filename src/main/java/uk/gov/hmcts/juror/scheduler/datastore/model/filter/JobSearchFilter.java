package uk.gov.hmcts.juror.scheduler.datastore.model.filter;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;

import java.util.Set;

@Builder
@Getter
public class JobSearchFilter {

    private String jobKey;

    private Set<@Length(max = APIConstants.DEFAULT_MAX_LENGTH_SHORT) @NotBlank String> tags;

    private Boolean enabled;
}
