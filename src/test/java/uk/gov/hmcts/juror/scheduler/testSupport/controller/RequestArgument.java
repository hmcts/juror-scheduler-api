package uk.gov.hmcts.juror.scheduler.testsupport.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.function.Consumer;

@Setter
@Accessors(chain = true)
public class RequestArgument implements Arguments {
    private Consumer<MockHttpServletRequestBuilder> preActions = (builder) -> {
    };
    private Consumer<ResultActions> postActions = resultActions -> {
    };

    @Getter
    private final String requestPayload;

    @Getter
    private MediaType contentType = MediaType.APPLICATION_JSON;

    public RequestArgument(Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions) {
        this(preActions, postActions, null);
    }

    public RequestArgument(Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions,
                           String requestPayload) {
        if (preActions != null) {
            this.preActions = preActions;
        }
        if (postActions != null) {
            this.postActions = postActions;
        }
        this.requestPayload = requestPayload;
    }

    public void runPreActions(MockHttpServletRequestBuilder builder, ControllerTest controllerTest) throws Exception {
        this.preActions.accept(builder);
    }

    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        this.postActions.accept(resultActions);
    }


    @Override
    public final Object[] get() {
        return new Object[]{this};
    }
}
