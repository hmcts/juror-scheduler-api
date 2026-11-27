package uk.gov.hmcts.juror.scheduler.config.scheduler;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenApiCustomizer routeSecurity() {
        return api -> api.getPaths().forEach((path, item) -> {
            if (item.getParameters() != null) {
                item.getParameters().removeIf(parameter -> "header".equals(parameter.getIn())
                    && "Authorization".equalsIgnoreCase(parameter.getName()));
            }
            item.readOperationsMap().forEach((method, operation) -> {
                if (operation.getParameters() != null) {
                    operation.getParameters().removeIf(parameter -> "header".equals(parameter.getIn())
                        && "Authorization".equalsIgnoreCase(parameter.getName()));
                }
                // Keep aligned with the shared library's SecurityConfig and method security.
                boolean publicRoute = method == PathItem.HttpMethod.GET
                    && ("/".equals(path) || "/health".equals(path) || path.startsWith("/health/")
                    || "/info".equals(path) || "/metrics".equals(path) || path.startsWith("/metrics/"))
                    || method == PathItem.HttpMethod.POST && "/auth/login".equals(path);
                operation.setSecurity(publicRoute ? List.of()
                    : List.of(new SecurityRequirement().addList(BEARER_AUTH)));
            });
        });
    }

    @Bean
    public OperationCustomizer permissionDocumentation() {
        return (operation, handler) -> {
            PreAuthorize permission = handler.getMethodAnnotation(PreAuthorize.class);
            if (permission != null) {
                operation.setDescription((operation.getDescription() == null ? "" : operation.getDescription())
                    + "\nRequired authorization: `" + permission.value() + "`.");
            }
            return operation;
        };
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    .description("Bearer JWT issued by POST /auth/login. Route permissions also apply.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .info(new Info().title("Scheduler Specifications")
                        .description("A generic Scheduler application to trigger jobs at set intervals")
                        .version("v0.0.1")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                )
                //TODO change to link to our readme
                //.externalDocs(new ExternalDocumentation()
                //        .description("README")
                //        .url("https://github.com/hmcts/spring-boot-template"))
                ;
    }
}
