package be.steby.CoreProject.il.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "security.email")
@Getter
@Setter
public class EmailSecurityProperties {

    /**
     * Indique si la whitelist des domaines est requise
     */
    private boolean requireDomainWhitelist = false;

    /**
     * Liste des domaines autorisés (si whitelist activée)
     */
    private Set<String> allowedDomains = new HashSet<>();

    /**
     * Liste des domaines interdits
     */
    private Set<String> blacklistedDomains = new HashSet<>();

    /**
     * Limitation des changements d'email
     */
    private int maxChangesPerDay = 3;
    private int maxChangesPerMonth = 10;

    /**
     * Limitation des demandes de changement
     */
    private int maxChangeRequests = 5;
    private int lockoutMinutes = 30;
}