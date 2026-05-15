package be.steby.CoreProject.il.telephony;

import be.steby.CoreProject.dal.repositories.crm.TelephonyConfigRepository;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the active {@link TelephonyPort} adapter at runtime.
 *
 * <p>Spring injects all registered {@link TelephonyPort} implementations.
 * On each resolution, the active {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig}
 * is read from the database to determine which adapter to return.
 * If no configuration is found, {@link CallProvider#TEL_URI} is used as the fallback —
 * it requires no credentials and works for every tenant.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   TelephonyPort adapter = resolver.resolve();
 *   CallSession session = adapter.initiate(command);
 * </pre>
 *
 * @see TelephonyPort
 * @see be.steby.CoreProject.dl.entities.crm.TelephonyConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelephonyAdapterResolver {

    private final List<TelephonyPort> adapters;
    private final TelephonyConfigRepository configRepository;

    /**
     * Returns the adapter matching the tenant's active telephony configuration.
     *
     * <p>Falls back to {@link CallProvider#TEL_URI} when no configuration record
     * is active, ensuring calls can always be initiated without setup.</p>
     *
     * @return the active {@link TelephonyPort} adapter
     * @throws IllegalStateException if no adapter is registered for the configured provider
     */
    public TelephonyPort resolve() {
        CallProvider provider = configRepository.findByActiveTrue()
                .map(config -> {
                    log.debug("Telephony provider resolved from config: {}", config.getProvider());
                    return config.getProvider();
                })
                .orElseGet(() -> {
                    log.debug("No active telephony config found — falling back to TEL_URI");
                    return CallProvider.TEL_URI;
                });

        return adapters.stream()
                .filter(adapter -> adapter.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No TelephonyPort adapter registered for provider: " + provider));
    }
}
