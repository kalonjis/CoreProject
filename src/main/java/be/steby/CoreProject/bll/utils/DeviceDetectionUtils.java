package be.steby.CoreProject.bll.utils;

import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class DeviceDetectionUtils {

    /**
     * Détermine le type d'appareil à partir des données d'analyse
     */
    public static String determineDeviceType(UserAgent agent) {
        // Vérifier le champ DeviceClass qui indique spécifiquement le type d'appareil
        String deviceClass = agent.getValue("DeviceClass");
        if (deviceClass != null && !deviceClass.isEmpty()) {
            if (deviceClass.equalsIgnoreCase("Phone")) {
                return "MOBILE";
            } else if (deviceClass.equalsIgnoreCase("Tablet")) {
                return "TABLET";
            } else if (deviceClass.equalsIgnoreCase("Desktop")) {
                return "DESKTOP";
            }
        }

        // Si DeviceClass n'est pas disponible ou ne correspond pas à nos catégories,
        // utiliser le champ DeviceType pour plus de précision
        String deviceType = agent.getValue("DeviceType");
        if (deviceType != null && !deviceType.isEmpty()) {
            switch (deviceType.toLowerCase()) {
                case "mobile":
                case "phone":
                case "smartphone":
                    return "MOBILE";
                case "tablet":
                    return "TABLET";
                case "desktop":
                case "pc":
                    return "DESKTOP";
                case "watch":
                case "wearable":
                    return "WEARABLE";
                case "tv":
                case "smart tv":
                case "television":
                    return "TV";
                case "game console":
                case "gaming console":
                    return "GAME_CONSOLE";
            }
        }

        // Utiliser la forme du facteur comme fallback
        String formFactor = agent.getValue("DeviceFormFactor");
        if (formFactor != null && !formFactor.isEmpty()) {
            if (formFactor.contains("Smartphone") || formFactor.contains("Phone")) {
                return "MOBILE";
            } else if (formFactor.contains("Tablet")) {
                return "TABLET";
            } else if (formFactor.contains("Desktop")) {
                return "DESKTOP";
            }
        }

        // En dernier recours, vérifier le système d'exploitation
        String os = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        if (os != null) {
            if (os.contains("Android") || os.contains("iOS") || os.contains("iPhone OS")) {
                // Vérifier si c'est une tablette Android ou un iPad
                String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
                if ((os.contains("Android") && (deviceName != null && deviceName.contains("Tablet"))) ||
                        (os.contains("iOS") && deviceName != null && deviceName.contains("iPad"))) {
                    return "TABLET";
                }
                return "MOBILE";
            } else if (os.contains("Windows") || os.contains("Mac OS X") || os.contains("Linux")) {
                return "DESKTOP";
            }
        }

        return "UNKNOWN";
    }

    /**
     * Extrait la version du système d'exploitation de l'agent utilisateur
     */
    public static String getOsVersion(UserAgent agent) {
        String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);

        // Si la version est "??" ou vide, essayer d'autres méthodes d'extraction
        if (osVersion == null || osVersion.isEmpty() || "??".equals(osVersion)) {
            // Essayer d'extraire depuis des champs alternatifs de YAUAA
            osVersion = agent.getValue("OperatingSystemVersionBuild");

            if (osVersion == null || osVersion.isEmpty()) {
                osVersion = agent.getValue("OperatingSystemNameVersion");
                if (osVersion != null && !osVersion.isEmpty()) {
                    // Tenter d'extraire juste la partie version
                    String[] parts = osVersion.split(" ");
                    if (parts.length > 1) {
                        // Prendre le dernier élément comme version potentielle
                        osVersion = parts[parts.length - 1];
                    }
                }
            }

            // En dernier recours, essayer d'extraire manuellement depuis le User-Agent brut
            if (osVersion == null || osVersion.isEmpty() || "??".equals(osVersion)) {
                String ua = agent.getUserAgentString();
                if (ua != null) {
                    // Pour Windows
                    if (ua.contains("Windows NT")) {
                        int start = ua.indexOf("Windows NT") + 11;
                        int end = ua.indexOf(")", start);
                        if (start > 0 && end > start) {
                            osVersion = ua.substring(start, end).trim();
                            // Convertir les versions NT en noms conviviaux
                            switch (osVersion) {
                                case "10.0": return "10";
                                case "6.3": return "8.1";
                                case "6.2": return "8";
                                case "6.1": return "7";
                                case "6.0": return "Vista";
                                case "5.2": return "XP 64-bit";
                                case "5.1": return "XP";
                            }
                        }
                    }
                    // Pour macOS
                    else if (ua.contains("Mac OS X")) {
                        int start = ua.indexOf("Mac OS X") + 9;
                        int end = ua.indexOf(")", start);
                        if (end == -1) end = ua.indexOf(";", start);
                        if (start > 0 && end > start) {
                            osVersion = ua.substring(start, end).trim();
                            // Nettoyer la version
                            osVersion = osVersion.replace("_", ".");
                        }
                    }
                    // Pour Android
                    else if (ua.contains("Android")) {
                        int start = ua.indexOf("Android") + 8;
                        int end = ua.indexOf(";", start);
                        if (end == -1) end = ua.indexOf(" ", start);
                        if (start > 0 && end > start) {
                            osVersion = ua.substring(start, end).trim();
                        }
                    }
                }
            }
        }

        return osVersion == null ? "Unknown" : osVersion;
    }

    /**
     * Récupère la langue préférée de l'utilisateur à partir de la requête HTTP
     */
    public static String getAcceptedLanguage(HttpServletRequest request) {
        String language = "Unknown";
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if ("accept-language".equalsIgnoreCase(headerName)) {
                String acceptLanguage = request.getHeader(headerName);
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    String[] languages = acceptLanguage.split(",");
                    if (languages.length > 0) {
                        language = languages[0];
                        break;
                    }
                }
            }
        }
        return language;
    }

    /**
     * Génère une empreinte unique pour l'appareil
     */
    public static String generateFingerprint(HttpServletRequest request, Long userId) {
        StringBuilder fingerprint = new StringBuilder();

        // Informations de base
        String userAgent = request.getHeader("User-Agent");
        fingerprint.append(userAgent != null ? userAgent : "unknown");

        // Ajout de l'IP (même si l'IP peut changer, elle reste utile)
        String ip = IpUtils.getClientIp(request);
        fingerprint.append("_").append(ip);

        // En-têtes HTTP additionnels
        String acceptHeader = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");

        fingerprint.append("_").append(acceptHeader != null ? acceptHeader : "")
                .append("_").append(acceptLanguage != null ? acceptLanguage : "")
                .append("_").append(acceptEncoding != null ? acceptEncoding : "");

        // Inclure le fuseau horaire et la résolution d'écran si disponibles
        // (ces informations pourraient être envoyées par le client via des en-têtes personnalisés)
        String timezone = request.getHeader("X-Timezone");
        String screenInfo = request.getHeader("X-Screen-Info");

        if (timezone != null) {
            fingerprint.append("_").append(timezone);
        }

        if (screenInfo != null) {
            fingerprint.append("_").append(screenInfo);
        }

        // Inclure l'ID de l'utilisateur
        fingerprint.append("_").append(userId);

        // Générer un hash pour réduire la taille et masquer les informations brutes
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(fingerprint.toString().getBytes(StandardCharsets.UTF_8));

            // Convertir le hash en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback si SHA-256 n'est pas disponible
            return fingerprint.toString().hashCode() + "_" + userId;
        }
    }

    /**
     * Extrait les informations d'appareil à partir de l'agent utilisateur
     */
    public static void populateDeviceInfo(Device device, UserAgent agent, HttpServletRequest request) {
        device.setDeviceType(determineDeviceType(agent));
        device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));
        device.setBrowserVersion(agent.getValue(UserAgent.AGENT_VERSION));
        device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        device.setOsVersion(getOsVersion(agent));
        device.setDevice_cpu(agent.getValue(UserAgent.DEVICE_CPU));
        device.setDevice_cpu_bits(agent.getValue(UserAgent.DEVICE_CPU_BITS));
        device.setLanguage(getAcceptedLanguage(request));
        device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
        device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
    }
}
