package be.steby.CoreProject.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for @SpringBootTest integration tests.
 *
 * <p>Boots the full Spring context against an in-memory H2 database (via the
 * "integration" profile), disables Flyway and rate limiting, and configures
 * MockMvc for in-process HTTP testing with the real security filter chain.
 *
 * <p>No network, no PostgreSQL, no SSL — but real JWT filters, CSRF, and @PreAuthorize.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "integration"})
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
