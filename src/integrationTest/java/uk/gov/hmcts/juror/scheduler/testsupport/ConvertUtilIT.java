package uk.gov.hmcts.juror.scheduler.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConvertUtilIT {
    private ConvertUtilIT() {
    }

    public static <T> String mapObjectToJson(T object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }
}
