package be.steby.CoreProject.il.telephony;

import be.steby.CoreProject.dal.repositories.crm.CommercialSipConfigRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the active {@link TelephonyPort} adapter at runtime.
 *
 * <h3>Resolution priority (per user)</h3>
 * <ol>
 *   <li>User has a per-user {@code CommercialSipConfig} → {@link CallProvider#SIP}</li>
 *   <li>Global provider from {@code telephony.global-provider} (env: {@code TELEPHONY_GLOBAL_PROVIDER})</li>
 *   <li>Default → {@link CallProvider#TEL_URI} (safe fallback)</li>
 * </ol>
 *
 * <h3>Session termination</h3>
 * <p>Use {@link #resolveByProvider(CallProvider)} when the provider is already
 * known (stored on the {@code CallSession}). This avoids a second lookup and
 * is immune to config changes that happen between initiation and termination.</p>
 *
 * @see TelephonyPort
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelephonyAdapterResolver {

    private final List<TelephonyPort>           adapters;
    private final CommercialSipConfigRepository sipConfigRepository;

    @Value("${telephony.global-provider:TEL_URI}")
    private String globalProvider;

    /**
     * Resolves the adapter for the given user, respecting per-user SIP priority.
     *
     * @param actor the authenticated user placing the call
     * @return the appropriate {@link TelephonyPort} adapter
     */
    public TelephonyPort resolve(User actor) {
        if (sipConfigRepository.existsByUserId(actor.getId())) {
            log.debug("Per-user SIP config found for user {} — using SIP adapter", actor.getId());
            return resolveByProvider(CallProvider.SIP);
        }
        return resolve();
    }

    /**
     * Resolves the adapter that matches the given explicit provider.
     * Use this when the provider is already known (e.g. stored on {@code CallSession}).
     *
     * @param provider the provider enum value to match
     * @return the matching {@link TelephonyPort} adapter
     * @throws IllegalStateException if no adapter is registered for the given provider
     */
    public TelephonyPort resolveByProvider(CallProvider provider) {
        return adapters.stream()
                .filter(adapter -> adapter.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No TelephonyPort adapter registered for provider: " + provider));
    }

    /**
     * Resolves the adapter matching the tenant's global telephony configuration.
     * Reads {@code telephony.global-provider} from yml; falls back to {@link CallProvider#TEL_URI}.
     *
     * @return the active {@link TelephonyPort} adapter
     */
    public TelephonyPort resolve() {
        CallProvider provider;
        try {
            provider = CallProvider.valueOf(globalProvider);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown telephony.global-provider '{}' — falling back to TEL_URI", globalProvider);
            provider = CallProvider.TEL_URI;
        }
        log.debug("Telephony provider resolved from config: {}", provider);
        return resolveByProvider(provider);
    }
}
