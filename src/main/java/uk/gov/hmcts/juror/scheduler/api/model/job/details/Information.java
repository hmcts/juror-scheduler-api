package uk.gov.hmcts.juror.scheduler.api.model.job.details;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;
import uk.gov.hmcts.juror.scheduler.api.APIConstants;

import java.util.Set;

@Getter
@Builder
public class Information {

    @NotBlank
    @Length(max = APIConstants.DEFAULT_MAX_LENGTH_LONG)
    private String name;
    @Length(max = APIConstants.DEFAULT_MAX_LENGTH_LONG)

    private String description;

    private Set<
        @Length(max = APIConstants.DEFAULT_MAX_LENGTH_SHORT) @NotBlank String> tags;
}
