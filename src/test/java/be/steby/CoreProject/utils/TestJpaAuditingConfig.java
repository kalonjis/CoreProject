package be.steby.CoreProject.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * JPA Auditing configuration for @DataJpaTest slices.
 *
 * CoreProjectApplication already registers @EnableJpaAuditing(auditorAwareRef = "auditAwareImpl").
 * This config simply provides the "auditAwareImpl" bean that @DataJpaTest would otherwise not load
 * (since it excludes @Component beans outside the JPA slice).
 */
@TestConfiguration
public class TestJpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditAwareImpl() {
        return () -> Optional.of("test");
    }
}
