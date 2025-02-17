package be.steby.CoreProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class CoreProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreProjectApplication.class, args);
	}

}
