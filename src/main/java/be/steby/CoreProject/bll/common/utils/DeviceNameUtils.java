package be.steby.CoreProject.bll.domains.device.utils;

import nl.basjes.parse.useragent.UserAgent;

public class DeviceNameUtils {

    /**
     * Génère un nom de device lisible et cohérent.
     * Priorise le device name détecté, sinon génère un nom basé sur les infos disponibles.
     */
    public static String generateDeviceName(UserAgent agent) {
        // 1. Essayer d'utiliser le device name détecté s'il est valide
        String detectedName = agent.getValue(UserAgent.DEVICE_NAME);
        if (isValidValue(detectedName)) {
            return detectedName;
        }

        // 2. Sinon, générer un nom basé sur les infos disponibles
        return generateFallbackDeviceName(agent);
    }

    /**
     * Génère un nom de device basé sur les informations disponibles
     */
    private static String generateFallbackDeviceName(UserAgent agent) {
        StringBuilder name = new StringBuilder();

        String deviceBrand = agent.getValue(UserAgent.DEVICE_BRAND);
        String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
        String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String browserName = agent.getValue(UserAgent.AGENT_NAME);

        // Commencer par la marque si disponible
        if (isValidValue(deviceBrand)) {
            name.append(deviceBrand);
        }

        // Ajouter la classe de device si disponible
        if (isValidValue(deviceClass)) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(capitalizeFirst(deviceClass));
        }

        // Si on n'a toujours rien, utiliser OS + Browser
        if (name.length() == 0) {
            if (isValidValue(osName)) {
                name.append(osName);
            }

            if (isValidValue(browserName)) {
                if (name.length() > 0) {
                    name.append(" - ");
                }
                name.append(browserName);
            }
        }

        // Fallback final si vraiment rien n'est disponible
        if (name.length() == 0) {
            return "Unknown Device";
        }

        return name.toString().trim();
    }

    /**
     * Génère un nom de device pour l'affichage dans l'interface utilisateur
     * (plus détaillé que pour le fingerprint)
     */
    public static String generateDisplayName(UserAgent agent) {
        String baseName = generateDeviceName(agent);

        // Ajouter plus de détails pour l'affichage si disponibles
        String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);

        StringBuilder displayName = new StringBuilder(baseName);

        // Ajouter l'OS si il n'est pas déjà dans le nom
        if (isValidValue(osName) && !baseName.toLowerCase().contains(osName.toLowerCase())) {
            displayName.append(" (").append(osName);

            if (isValidValue(osVersion) && !"UNDEFINED".equals(osVersion)) {
                displayName.append(" ").append(osVersion);
            }

            displayName.append(")");
        }

        return displayName.toString();
    }

    /**
     * Vérifie si une valeur est valide (non null, non vide, non "Unknown", non "UNDEFINED", etc.)
     */
    private static boolean isValidValue(String value) {
        return value != null
                && !value.isEmpty()
                && !value.equals("??")
                && !value.equals("Unknown")
                && !value.equals("UNDEFINED")
                && !value.equals("Hacker");  // Cas spécial parfois retourné
    }

    /**
     * Met en forme un mot avec la première lettre en majuscule
     */
    private static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}