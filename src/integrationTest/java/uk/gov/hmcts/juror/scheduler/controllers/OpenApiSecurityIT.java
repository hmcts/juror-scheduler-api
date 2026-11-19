package uk.gov.hmcts.juror.scheduler.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OpenApiSecurityIT extends AbstractIT {

    @Autowired
    OpenApiSecurityIT(MockMvc mockMvc) {
        super(mockMvc);
    }

    @Test
    void generatedSpecificationMatchesSecurity() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode api = new ObjectMapper().readTree(json);
        assertThat(api.path("openapi").asText()).startsWith("3.");
        JsonNode scheme = api.at("/components/securitySchemes/bearerAuth");
        assertThat(scheme.path("type").asText()).isEqualTo("http");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(api.path("security").get(0).has("bearerAuth")).isTrue();
        for (String publicPath : Set.of("/health", "/info", "/auth/login")) {
            JsonNode operation = api.path("paths").path(publicPath)
                .path("/auth/login".equals(publicPath) ? "post" : "get");
            assertThat(operation.isMissingNode()).as(publicPath).isFalse();
            assertThat(operation.path("security").isArray()).isTrue();
            assertThat(operation.path("security").size()).isZero();
        }
        for (String publicPath : Set.of("/health", "/info")) {
            mockMvc.perform(get(publicPath)).andExpect(status().isOk());
        }
        var paths = api.path("paths").fields();
        int protectedOperations = 0;
        while (paths.hasNext()) {
            var path = paths.next();
            var operations = path.getValue().fields();
            while (operations.hasNext()) {
                var entry = operations.next();
                if (!Set.of("get", "post", "put", "patch", "delete", "head", "options")
                    .contains(entry.getKey())) {
                    continue;
                }
                JsonNode operation = entry.getValue();
                for (JsonNode parameter : operation.path("parameters")) {
                    assertThat(parameter.path("name").asText()).isNotEqualToIgnoringCase("Authorization");
                }
                if (operation.path("security").isEmpty()) {
                    continue;
                }
                assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
                String url = path.getKey().replaceAll("\\{[^}]+}", "1");
                HttpMethod method = HttpMethod.valueOf(entry.getKey().toUpperCase(java.util.Locale.ROOT));
                String body = url.startsWith("/auth/") ? """
                    {"email":"security-check@example.com","password":"testPassword123",
                    "firstname":"Security","lastname":"Check"}
                    """ : "{}";
                mockMvcPerform(url, "", method, body).andExpect(status().isUnauthorized());
                mockMvcPerform(url, "invalid.jwt.signature", method, body)
                    .andExpect(status().isUnauthorized());
                protectedOperations++;
            }
        }
        assertThat(protectedOperations).isGreaterThan(10);
    }
}
