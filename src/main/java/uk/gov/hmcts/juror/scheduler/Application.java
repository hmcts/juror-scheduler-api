package uk.gov.hmcts.juror.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.juror.standard.config.ConfigProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ConfigProperties.class)
@ComponentScan("uk.gov.hmcts.juror")
@EntityScan("uk.gov.hmcts.juror")
@EnableJpaRepositories("uk.gov.hmcts.juror")
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
