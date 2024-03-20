package uk.gov.hmcts.juror.scheduler.api.model.job.details.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class APIJobDetailsResponse extends APIJobDetails {


    @JsonProperty("created_at")
    @Schema(description = "The time at which this job was created")
    private LocalDateTime createdAt;


    @JsonProperty("last_updated_at")
    @Schema(description = "The time at which this Job was last updated")
    private LocalDateTime lastUpdatedAt;

    @NotNull
    private Boolean enabled;
}
