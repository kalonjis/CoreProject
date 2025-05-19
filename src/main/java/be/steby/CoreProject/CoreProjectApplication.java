package be.steby.CoreProject;

import be.steby.CoreProject.il.Jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(JwtProperties.class)
public class CoreProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreProjectApplication.class, args);
	}

}
