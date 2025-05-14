package be.steby.CoreProject.bll.utils.device;

import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DeviceDetectionUtils {

    /**
     * Génère une empreinte unique pour l'appareil SANS utiliser l'adresse IP
     * pour éviter de créer de nouveaux devices à chaque changement d'IP
     *
     * Cette méthode utilise uniquement les headers HTTP standard disponibles
     * pour fonctionner avec n'importe quel client (Angular, Postman, etc.)
     */
    public static String generateFingerprint(HttpServletRequest request, Long userId) {
        StringBuilder fingerprint = new StringBuilder();

        // 1. User Agent - l'information la plus stable et toujours disponible
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            UserAgentAnalyzer analyzer = UserAgentAnalyzer.newBuilder()
                    .withCache(1000)
                    .build();
            UserAgent agent = analyzer.parse(userAgent);

            // Extraire des éléments spécifiques qui sont stables
            String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String osVersionMajor = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);
            String browserName = agent.getValue(UserAgent.AGENT_NAME);
            String browserVersionMajor = agent.getValue(UserAgent.AGENT_VERSION_MAJOR);
            String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
            String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
            String deviceBrand = agent.getValue(UserAgent.DEVICE_BRAND);

            // Ajouter seulement les valeurs non-null et valides
            appendIfValid(fingerprint, "OS", osName);

            // Ne pas utiliser la version OS si elle est UNDEFINED
            if (osVersionMajor != null && !"UNDEFINED".equals(osVersionMajor)) {
                appendIfValid(fingerprint, "OSVer", osVersionMajor);
            }

            appendIfValid(fingerprint, "Browser", browserName);

            // Ne pas utiliser la version du navigateur dans le fingerprint
            // car elle peut ne pas être disponible lors de certaines requêtes
            // appendIfValid(fingerprint, "BrowserVer", browserVersionMajor);

            appendIfValid(fingerprint, "DeviceClass", deviceClass);
            appendIfValid(fingerprint, "DeviceName", deviceName);
            appendIfValid(fingerprint, "DeviceBrand", deviceBrand);
        } else {
            fingerprint.append("UserAgent:unknown");
        }

        // 2. En-têtes HTTP stables (souvent constants pour un même appareil)
        String acceptLanguage = normalizeAcceptLanguage(request.getHeader("Accept-Language"));
        String acceptEncoding = request.getHeader("Accept-Encoding");

        appendIfValid(fingerprint, "Lang", acceptLanguage);
        appendIfValid(fingerprint, "Encoding", acceptEncoding);

        // 3. Headers additionnels s'ils sont disponibles (pour clients avancés comme Angular)
        String screenResolution = request.getHeader("X-Screen-Resolution");
        String timezone = request.getHeader("X-Timezone");
        String platform = request.getHeader("X-Platform");

        if (screenResolution != null) {
            appendIfValid(fingerprint, "Screen", screenResolution);
        }
        if (timezone != null) {
            appendIfValid(fingerprint, "TZ", timezone);
        }
        if (platform != null) {
            appendIfValid(fingerprint, "Platform", platform);
        }

        // 4. ID utilisateur pour éviter les collisions entre utilisateurs
        fingerprint.append("_User:").append(userId);

        // Debug log
        System.out.println("Generated fingerprint components: " + fingerprint.toString());

        // 5. Générer un hash SHA-256 de l'empreinte
        return generateSecureHash(fingerprint.toString());
    }

    /**
     * Ajoute une valeur au fingerprint seulement si elle est valide
     */
    private static void appendIfValid(StringBuilder fingerprint, String key, String value) {
        if (value != null && !value.isEmpty() && !value.equals("??") && !value.equals("Unknown") && !value.equals("UNDEFINED")) {
            fingerprint.append("_").append(key).append(":").append(value);
        }
    }

    /**
     * Normalise Accept-Language pour une comparaison stable
     */
    private static String normalizeAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return "unknown";
        }

        // Prendre seulement la langue principale (avant la virgule)
        if (acceptLanguage.contains(",")) {
            acceptLanguage = acceptLanguage.split(",")[0];
        }

        // Retirer la qualité (q=0.9)
        if (acceptLanguage.contains(";")) {
            acceptLanguage = acceptLanguage.split(";")[0];
        }

        return acceptLanguage.trim().toLowerCase();
    }

    /**
     * Génère un hash SHA-256 sécurisé de la chaîne d'empreinte
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
            // Fallback si SHA-256 n'est pas disponible
            return String.valueOf(data.hashCode()) + "_" + System.currentTimeMillis();
        }
    }

    /**
     * Détermine le type d'appareil à partir des données d'analyse
     */
    public static String determineDeviceType(UserAgent agent) {
        // Vérifier le champ DeviceClass qui indique spécifiquement le type d'appareil
        String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
        if (deviceClass != null && !deviceClass.isEmpty()) {
            switch (deviceClass.toLowerCase()) {
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

        // Si DeviceClass n'est pas disponible, utiliser DeviceType
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

        // En dernier recours, analyser le système d'exploitation
        String os = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        if (os != null) {
            if (os.contains("Android") || os.contains("iOS") || os.contains("iPhone OS")) {
                // Vérifier si c'est une tablette
                String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
                if (deviceName != null && (deviceName.contains("iPad") || deviceName.contains("Tablet"))) {
                    return "TABLET";
                }
                return "MOBILE";
            } else if (os.contains("Windows") || os.contains("Mac OS X") || os.contains("Linux") || os.contains("Ubuntu")) {
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
            // Essayer d'extraire depuis des champs alternatifs
            osVersion = agent.getValue("OperatingSystemVersionBuild");

            if (osVersion == null || osVersion.isEmpty()) {
                osVersion = agent.getValue("OperatingSystemNameVersion");
                if (osVersion != null && !osVersion.isEmpty()) {
                    // Tenter d'extraire juste la partie version
                    String[] parts = osVersion.split(" ");
                    if (parts.length > 1) {
                        osVersion = parts[parts.length - 1];
                    }
                }
            }
        }

        // Retourner "Unknown" au lieu de "UNDEFINED" pour la cohérence
        return osVersion != null && !osVersion.isEmpty() && !"??".equals(osVersion) ? osVersion : "Unknown";
    }

    /**
     * Récupère la langue préférée de l'utilisateur à partir de la requête HTTP
     */
    public static String getAcceptedLanguage(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            return normalizeAcceptLanguage(acceptLanguage);
        }
        return "Unknown";
    }

    /**
     * Extrait les informations d'appareil à partir de l'agent utilisateur
     * et remplit l'objet Device avec toutes les informations disponibles
     */
    public static void populateDeviceInfo(Device device, UserAgent agent, HttpServletRequest request) {
        device.setDeviceType(determineDeviceType(agent));
        device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));

        // Stocker la version du navigateur seulement si elle est disponible
        String browserVersion = agent.getValue(UserAgent.AGENT_VERSION);
        device.setBrowserVersion(browserVersion != null && !browserVersion.isEmpty() ? browserVersion : null);

        device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        device.setOsVersion(getOsVersion(agent));
        device.setDevice_cpu(agent.getValue(UserAgent.DEVICE_CPU));
        device.setDevice_cpu_bits(agent.getValue(UserAgent.DEVICE_CPU_BITS));
        device.setLanguage(getAcceptedLanguage(request));
        device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
        device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
    }
}