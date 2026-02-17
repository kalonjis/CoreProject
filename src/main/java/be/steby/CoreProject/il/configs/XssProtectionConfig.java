package be.steby.CoreProject.il.configs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Global XSS protection via Jackson deserialization.
 *
 * <p>This configuration automatically sanitizes ALL incoming JSON string fields
 * by rejecting values containing HTML tags or dangerous patterns.</p>
 *
 * <h4>How it works:</h4>
 * <ol>
 *   <li>Registers a custom String deserializer with Jackson</li>
 *   <li>Every JSON string field passes through the deserializer</li>
 *   <li>If HTML/XSS patterns detected → throws exception → 400 Bad Request</li>
 * </ol>
 *
 * <h4>Advantages:</h4>
 * <ul>
 *   <li>No need to add @NoHtml on every field</li>
 *   <li>Applies to ALL endpoints automatically</li>
 *   <li>Cannot be forgotten on new DTOs</li>
 *   <li>Single point of maintenance</li>
 * </ul>
 *
 * <h4>Bypassing (if needed):</h4>
 * <p>If you have fields that legitimately need HTML (e.g., rich text editor),
 * use @JsonDeserialize(using = JsonDeserializer.None.class) on that specific field.</p>
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Configuration
@Slf4j
public class XssProtectionConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xssProtectionCustomizer() {
        return builder -> {
            SimpleModule xssModule = new SimpleModule("XssProtectionModule");
            xssModule.addDeserializer(String.class, new XssSanitizingDeserializer());

            // modulesToInstall AJOUTE aux modules existants (dont JavaTimeModule)
            // contrairement à modules() qui les REMPLACE
            builder.modulesToInstall(xssModule);

            log.info("XSS protection enabled for all JSON string fields");
        };
    }

    /**
     * Custom deserializer that checks strings for XSS patterns.
     */
    public static class XssSanitizingDeserializer extends JsonDeserializer<String> {

        // HTML tags: <script>, </div>, <img/>, etc.
        private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
                "<[^>]+>",
                Pattern.CASE_INSENSITIVE
        );

        // HTML entities: &lt;, &amp;, &#60;, &#x3C;
        private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile(
                "&[a-zA-Z][a-zA-Z0-9]*;" +   // Named: &amp;
                        "|&#[0-9]+;" +                // Decimal: &#60;
                        "|&#x[0-9a-fA-F]+;",          // Hex: &#x3C;
                Pattern.CASE_INSENSITIVE
        );

        // Dangerous XSS vectors
        private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
                "javascript\\s*:" +
                        "|vbscript\\s*:" +
                        "|on\\w+\\s*=",               // onclick=, onerror=, etc.
                Pattern.CASE_INSENSITIVE
        );

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();

            if (value == null || value.isEmpty()) {
                return value;
            }

            // Check for HTML tags
            if (HTML_TAG_PATTERN.matcher(value).find()) {
                String fieldName = p.getCurrentName();
                log.warn("XSS attempt blocked - HTML tag in field '{}': {}",
                        fieldName, truncate(value));
                throw new IllegalArgumentException(
                        "HTML tags are not allowed in field: " + fieldName);
            }

            // Check for HTML entities
            if (HTML_ENTITY_PATTERN.matcher(value).find()) {
                String fieldName = p.getCurrentName();
                log.warn("XSS attempt blocked - HTML entity in field '{}': {}",
                        fieldName, truncate(value));
                throw new IllegalArgumentException(
                        "HTML entities are not allowed in field: " + fieldName);
            }

            // Check for dangerous patterns
            if (DANGEROUS_PATTERN.matcher(value).find()) {
                String fieldName = p.getCurrentName();
                log.warn("XSS attempt blocked - dangerous pattern in field '{}': {}",
                        fieldName, truncate(value));
                throw new IllegalArgumentException(
                        "Invalid content in field: " + fieldName);
            }

            return value;
        }

        private String truncate(String value) {
            return value.length() > 50 ? value.substring(0, 50) + "..." : value;
        }
    }
}