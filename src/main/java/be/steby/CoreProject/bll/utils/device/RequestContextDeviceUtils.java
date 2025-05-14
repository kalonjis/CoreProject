package be.steby.CoreProject.bll.utils.device;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RequestContextDeviceUtils {

    /**
     * Génère exactement le même fingerprint que DeviceDetectionUtils
     * mais à partir d'un RequestContext (pour usage asynchrone)
     */
    public static String generateFingerprint(RequestContext context, Long userId) {
        StringBuilder fingerprint = new StringBuilder();

        // 1. Utiliser les infos device déjà parsées si disponibles
        RequestContext.CapturedDeviceInfo deviceInfo = context.getDeviceInfo();
        if (deviceInfo != null) {
            // Utiliser exactement la même logique que DeviceDetectionUtils
            appendIfValid(fingerprint, "OS", deviceInfo.getOsName());

            // Ne pas utiliser la version OS si elle est UNDEFINED
            if (deviceInfo.getOsVersionMajor() != null && !"UNDEFINED".equals(deviceInfo.getOsVersionMajor())) {
                appendIfValid(fingerprint, "OSVer", deviceInfo.getOsVersionMajor());
            }

            appendIfValid(fingerprint, "Browser", deviceInfo.getBrowserName());

            // NE PAS utiliser la version du navigateur dans le fingerprint
            // car elle peut être null ou non disponible
            // appendIfValid(fingerprint, "BrowserVer", deviceInfo.getBrowserVersionMajor());

            appendIfValid(fingerprint, "DeviceClass", deviceInfo.getDeviceClass());
            appendIfValid(fingerprint, "DeviceName", deviceInfo.getDeviceName());
            appendIfValid(fingerprint, "DeviceBrand", deviceInfo.getDeviceBrand());
        } else if (context.getUserAgent() != null) {
            // Si les infos n'ont pas été pré-parsées, parser maintenant
            UserAgentAnalyzer analyzer = UserAgentAnalyzer.newBuilder()
                    .withCache(1000)
                    .build();
            UserAgent agent = analyzer.parse(context.getUserAgent());

            appendIfValid(fingerprint, "OS", agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));

            // Ne pas utiliser la version OS si elle est UNDEFINED
            String osVersionMajor = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);
            if (osVersionMajor != null && !"UNDEFINED".equals(osVersionMajor)) {
                appendIfValid(fingerprint, "OSVer", osVersionMajor);
            }

            appendIfValid(fingerprint, "Browser", agent.getValue(UserAgent.AGENT_NAME));

            // NE PAS utiliser la version du navigateur
            // appendIfValid(fingerprint, "BrowserVer", agent.getValue(UserAgent.AGENT_VERSION_MAJOR));

            appendIfValid(fingerprint, "DeviceClass", agent.getValue(UserAgent.DEVICE_CLASS));
            appendIfValid(fingerprint, "DeviceName", agent.getValue(UserAgent.DEVICE_NAME));
            appendIfValid(fingerprint, "DeviceBrand", agent.getValue(UserAgent.DEVICE_BRAND));
        } else {
            fingerprint.append("UserAgent:unknown");
        }

        // 2. Headers HTTP stables (exactement comme DeviceDetectionUtils)
        RequestContext.CapturedHeaders headers = context.getHeaders();
        String acceptLanguage = normalizeAcceptLanguage(headers.getAcceptLanguage());
        String acceptEncoding = headers.getAcceptEncoding();

        appendIfValid(fingerprint, "Lang", acceptLanguage);
        appendIfValid(fingerprint, "Encoding", acceptEncoding);

        // 3. Headers additionnels s'ils sont disponibles
        if (headers.getXScreenResolution() != null) {
            appendIfValid(fingerprint, "Screen", headers.getXScreenResolution());
        }
        if (headers.getXTimezone() != null) {
            appendIfValid(fingerprint, "TZ", headers.getXTimezone());
        }
        if (headers.getXPlatform() != null) {
            appendIfValid(fingerprint, "Platform", headers.getXPlatform());
        }

        // 4. ID utilisateur (exactement comme DeviceDetectionUtils)
        fingerprint.append("_User:").append(userId);

        // Debug log
        System.out.println("Generated fingerprint components (RequestContext): " + fingerprint.toString());

        // 5. Utiliser exactement la même méthode de hash
        return generateSecureHash(fingerprint.toString());
    }

    /**
     * Ajoute une valeur au fingerprint seulement si elle est valide
     * (copie exacte de DeviceDetectionUtils avec ajout de UNDEFINED)
     */
    private static void appendIfValid(StringBuilder fingerprint, String key, String value) {
        if (value != null && !value.isEmpty() && !value.equals("??") && !value.equals("Unknown") && !value.equals("UNDEFINED")) {
            fingerprint.append("_").append(key).append(":").append(value);
        }
    }

    /**
     * Normalise Accept-Language pour une comparaison stable
     * (copie exacte de DeviceDetectionUtils)
     */
    private static String normalizeAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return "unknown";
        }

        if (acceptLanguage.contains(",")) {
            acceptLanguage = acceptLanguage.split(",")[0];
        }

        if (acceptLanguage.contains(";")) {
            acceptLanguage = acceptLanguage.split(";")[0];
        }

        return acceptLanguage.trim().toLowerCase();
    }

    /**
     * Génère un hash SHA-256 sécurisé
     * (copie exacte de DeviceDetectionUtils)
     */
    private static String generateSecureHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(data.hashCode()) + "_" + System.currentTimeMillis();
        }
    }

    /**
     * Extrait les informations d'appareil et les stocke dans le Device
     */
    public static void populateDeviceInfo(Device device, UserAgent agent, RequestContext context) {
        RequestContext.CapturedDeviceInfo deviceInfo = context.getDeviceInfo();

        if (deviceInfo != null) {
            // Utiliser les infos déjà parsées
            device.setDeviceType(determineDeviceType(deviceInfo));
            device.setBrowser(deviceInfo.getBrowserName());
            device.setBrowserVersion(deviceInfo.getBrowserVersionMajor());
            device.setOperatingSystem(deviceInfo.getOsName());

            // Gérer UNDEFINED pour osVersion
            String osVersion = deviceInfo.getOsVersionMajor();
            device.setOsVersion(osVersion != null && !"UNDEFINED".equals(osVersion) ? osVersion : "Unknown");

            device.setDeviceClass(deviceInfo.getDeviceClass());
            device.setDeviceBrand(deviceInfo.getDeviceBrand());
        } else {
            // Utiliser l'agent si fourni
            device.setDeviceType(determineDeviceType(agent));
            device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));

            // Stocker la version du navigateur seulement si elle est disponible
            String browserVersion = agent.getValue(UserAgent.AGENT_VERSION);
            device.setBrowserVersion(browserVersion != null && !browserVersion.isEmpty() ? browserVersion : null);

            device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));

            // Gérer la version OS
            String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);
            device.setOsVersion(osVersion != null && !osVersion.isEmpty() && !"??".equals(osVersion) ? osVersion : "Unknown");

            device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
            device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
        }

        // Infos des headers
        device.setLanguage(normalizeAcceptLanguage(context.getHeaders().getAcceptLanguage()));

        // Ne pas utiliser de CPU info depuis RequestContext car pas disponible
        device.setDevice_cpu(null);
        device.setDevice_cpu_bits(null);
    }

    /**
     * Détermine le type d'appareil à partir des infos capturées
     */
    private static String determineDeviceType(RequestContext.CapturedDeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceClass() != null && !deviceInfo.getDeviceClass().isEmpty()) {
            switch (deviceInfo.getDeviceClass().toLowerCase()) {
                case "phone":
                    return "MOBILE";
                case "tablet":
                    return "TABLET";
                case "desktop":
                    return "DESKTOP";
                case "watch":
                    return "WEARABLE";
                case "tv":
                case "set-top box":
                    return "TV";
                case "game console":
                    return "GAME_CONSOLE";
            }
        }

        if (deviceInfo.getDeviceType() != null && !deviceInfo.getDeviceType().isEmpty()) {
            switch (deviceInfo.getDeviceType().toLowerCase()) {
                case "mobile":
                case "phone":
                case "smartphone":
                    return "MOBILE";
                case "tablet":
                    return "TABLET";
                case "desktop":
                case "pc":
                    return "DESKTOP";
                default:
                    break;
            }
        }

        return "UNKNOWN";
    }

    private static String determineDeviceType(UserAgent agent) {
        // Même logique que DeviceDetectionUtils
        String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
        if (deviceClass != null && !deviceClass.isEmpty()) {
            switch (deviceClass.toLowerCase()) {
                case "phone":
                    return "MOBILE";
                case "tablet":
                    return "TABLET";
                case "desktop":
                    return "DESKTOP";
                default:
                    break;
            }
        }

        String os = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        if (os != null) {
            if (os.contains("Android") || os.contains("iOS") || os.contains("iPhone OS")) {
                String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
                if (deviceName != null && (deviceName.contains("iPad") || deviceName.contains("Tablet"))) {
                    return "TABLET";
                }
                return "MOBILE";
            } else if (os.contains("Windows") || os.contains("Mac OS X") || os.contains("Linux")) {
                return "DESKTOP";
            }
        }

        return "UNKNOWN";
    }
}