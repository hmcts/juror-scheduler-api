package uk.gov.hmcts.juror.scheduler.config.scheduler;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
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
