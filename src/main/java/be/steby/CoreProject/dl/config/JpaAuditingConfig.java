package be.steby.CoreProject.dl.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for @CreatedDate / @LastModifiedDate / @CreatedBy fields.
 *
 * <p>Isolated in a dedicated @Configuration so that @WebMvcTest slices can exclude
 * it (JPA is not needed in the web layer) without breaking @DataJpaTest.
 *
 * @see be.steby.CoreProject.utils.TestJpaAuditingConfig for the test-time AuditorAware bean
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class JpaAuditingConfig {
}
