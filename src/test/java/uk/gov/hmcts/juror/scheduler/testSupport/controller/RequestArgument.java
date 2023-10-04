package uk.gov.hmcts.juror.scheduler.testSupport.controller;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class RequestArgument implements Arguments {
    private Consumer<MockHttpServletRequestBuilder> preActions = (builder) -> {
    };
    private Consumer<ResultActions> postActions = resultActions -> {
    };

    private final String requestPayload;

    private MediaType contentType = MediaType.APPLICATION_JSON;

    public RequestArgument(Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions) {
        this(preActions, postActions, null);
    }

    public RequestArgument(Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions, String requestPayload) {
        if (preActions != null) {
            this.preActions = preActions;
        }
        if (postActions != null) {
            this.postActions = postActions;
        }
        this.requestPayload = requestPayload;
    }

    public <T extends RequestArgument> T setContentType(MediaType mediaType) {
        contentType = mediaType;
        return (T) this;
    }

    public void runPreActions(MockHttpServletRequestBuilder builder, ControllerTest controllerTest) throws Exception {
        this.preActions.accept(builder);
    }

    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        this.postActions.accept(resultActions);
    }

    public <T extends RequestArgument> T setPostActions(Consumer<ResultActions> postActions) {
        this.postActions = postActions;
        return (T) this;
    }

    public <T extends RequestArgument> T setPreActions(Consumer<MockHttpServletRequestBuilder> preActions) {
        this.preActions = preActions;
        return (T) this;
    }

    public String getRequestPayload(){
        return this.requestPayload;
    }
    public MediaType getContentType(){
        return this.contentType;
    }

    @Override
    public Object[] get() {
        return new Object[]{this};
    }
}
