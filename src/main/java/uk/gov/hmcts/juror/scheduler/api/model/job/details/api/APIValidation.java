package uk.gov.hmcts.juror.scheduler.api.model.job.details.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.scheduler.datastore.model.ValidationType;

@Getter
@Setter
@NoArgsConstructor
@Schema(oneOf = {
    JsonPathAPIValidation.class,
    StatusCodeAPIValidation.class,
    MaxResponseTimeAPIValidation.class
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(JsonPathAPIValidation.class),
    @JsonSubTypes.Type(StatusCodeAPIValidation.class),
    @JsonSubTypes.Type(MaxResponseTimeAPIValidation.class)
})
@Slf4j
public abstract class APIValidation {

    @JsonIgnore
    public abstract ValidationType getType();

}
