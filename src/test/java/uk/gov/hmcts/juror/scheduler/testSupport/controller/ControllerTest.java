package uk.gov.hmcts.juror.scheduler.testsupport.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;


    protected final HttpMethod method;
    protected final String url;
    protected final HttpStatus successStatus;

    public ControllerTest(HttpMethod method, String url, HttpStatus successStatus) {
        this.method = method;
        this.url = url;
        this.successStatus = successStatus;
    }

    //TODO replace with manual converter
    protected <T> String createResponseStringFromObject(T apiJobDetailsResponses) throws JsonProcessingException {
        return objectMapper.writeValueAsString(apiJobDetailsResponses);
    }

    private MockHttpServletRequestBuilder buildRequest(RequestArgument requestArgument) {
        MockHttpServletRequestBuilder requestBuilder = request(method, url);
        if (requestArgument.getRequestPayload() != null) {
            requestBuilder.content(requestArgument.getRequestPayload());
        }
        if (requestArgument.getContentType() != null) {
            requestBuilder.contentType(requestArgument.getContentType());
        }
        return requestBuilder;
    }

    @SneakyThrows
    protected void callAndValidate(RequestArgument requestArgument) {
        MockHttpServletRequestBuilder requestBuilder = buildRequest(requestArgument);
        requestArgument.runPreActions(requestBuilder, this);
        ResultActions resultActions = this.mockMvc
            .perform(requestBuilder)
            .andDo(print());
        requestArgument.runPostActions(resultActions, this);
    }

}
