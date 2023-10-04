package uk.gov.hmcts.juror.scheduler.testSupport.controller;

import org.hamcrest.Matchers;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.juror.scheduler.testSupport.TestUtil;

import java.util.function.Consumer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SuppressWarnings("unchecked")

public class SuccessRequestArgument extends RequestArgument {

    private final String name;
    private final String expectedResponsePayload;

    private boolean hasResponse;


    public SuccessRequestArgument(String name, Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions,
                                  String requestPayload) {
        this(name, preActions, postActions, requestPayload, null);
    }

    public SuccessRequestArgument(String name, Consumer<MockHttpServletRequestBuilder> preActions, Consumer<ResultActions> postActions,
                                  String requestPayload, String expectedResponsePayload) {
        super(preActions, postActions, requestPayload);
        this.name = name;
        this.expectedResponsePayload = expectedResponsePayload;
        this.hasResponse = this.expectedResponsePayload != null;
    }

    public <T extends RequestArgument> T setHasResponse(boolean hasResponse) {
        this.hasResponse = hasResponse;
        return (T) this;
    }

    @Override
    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        resultActions.andExpect(status().is(controllerTest.successStatus.value()));
        if (hasResponse && expectedResponsePayload != null) {
            resultActions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(TestUtil.jsonMatcher(JSONCompareMode.NON_EXTENSIBLE, expectedResponsePayload));
        } else if (hasResponse) {
            resultActions.andExpect(content().string(Matchers.anything()));
        } else {
            resultActions.andExpect(content().string(Matchers.emptyOrNullString()));
        }
        super.runPostActions(resultActions, controllerTest);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
