package be.steby.CoreProject.il.webhook.registry;

import be.steby.CoreProject.il.webhook.config.WebhookProperties;
import be.steby.CoreProject.il.webhook.config.WebhookProperties.WebhookSourceConfig;
import be.steby.CoreProject.il.webhook.mapper.WebhookMapper;
import be.steby.CoreProject.il.webhook.verifier.WebhookVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central registry that resolves the correct {@link WebhookVerifier} and
 * {@link WebhookMapper} for a given source key (e.g. {@code "facebook"}).
 *
 * <p>Verifiers are keyed by {@link WebhookVerifier#algorithm()} and matched
 * against the {@code algorithm} field in the YAML config.</p>
 *
 * <p>Mappers are keyed by {@link WebhookMapper#source()} and matched
 * against the source path variable of the ingest endpoint.</p>
 */
@Component
@Slf4j
public class WebhookHandlerRegistry {

    private final WebhookProperties properties;
    private final Map<String, WebhookVerifier> verifiers;
    private final Map<String, WebhookMapper> mappers;

    public WebhookHandlerRegistry(
            WebhookProperties properties,
            List<WebhookVerifier> verifiers,
            List<WebhookMapper> mappers
    ) {
        this.properties = properties;
        this.verifiers  = verifiers.stream()
                .collect(Collectors.toMap(WebhookVerifier::algorithm, Function.identity()));
        this.mappers    = mappers.stream()
                .collect(Collectors.toMap(WebhookMapper::source, Function.identity()));

        log.info("WebhookHandlerRegistry initialized — sources: {}, verifiers: {}, mappers: {}",
                properties.getSources().keySet(), this.verifiers.keySet(), this.mappers.keySet());
    }

    /** Returns the config for a source key, or empty if not configured. */
    public Optional<WebhookSourceConfig> findConfig(String source) {
        return Optional.ofNullable(properties.getSources().get(source));
    }

    /** Returns the verifier matching the config's algorithm, or empty if not supported. */
    public Optional<WebhookVerifier> findVerifier(WebhookSourceConfig config) {
        return Optional.ofNullable(verifiers.get(config.getAlgorithm()));
    }

    /** Returns the mapper for a source key, or empty if not registered. */
    public Optional<WebhookMapper> findMapper(String source) {
        return Optional.ofNullable(mappers.get(source));
    }
}
