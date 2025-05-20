package be.steby.CoreProject.bll.domains.device.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Map;

public class IpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * Récupère l'adresse IP du client en vérifiant plusieurs en-têtes HTTP
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ipList = request.getHeader(header);
            if (ipList != null && ipList.length() > 0 && !"unknown".equalsIgnoreCase(ipList)) {
                // X-Forwarded-For peut contenir plusieurs IP séparées par des virgules
                // La première est généralement l'IP du client
                String[] ips = ipList.split(",");
                return ips[0].trim();
            }
        }
        // Si aucun en-tête n'est trouvé, on utilise l'IP de la requête directe
        return request.getRemoteAddr();
    }

    /**
     * Vérifie si une adresse IP est privée ou locale
     */
    public static boolean isPrivateIpAddress(String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            return addr.isLoopbackAddress() ||
                    addr.isSiteLocalAddress() ||
                    addr.isLinkLocalAddress() ||
                    addr.isAnyLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère la localisation géographique d'une adresse IP
     */
    public static String getLocationFromIp(String ipAddress) {
        // Vérifier si l'IP est privée ou locale
        if (isPrivateIpAddress(ipAddress)) {
            return "Local Network";
        }

        // Utiliser HTTPS au lieu de HTTP
        String url = "https://ip-api.com/json/" + ipAddress;
        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && "success".equals(response.get("status"))) {
                String city = (String) response.get("city");
                String region = (String) response.get("regionName");
                String country = (String) response.get("country");
                return (city != null ? city + ", " : "") +
                        (region != null ? region + ", " : "") +
                        (country != null ? country : "Unknown");
            }
        } catch (Exception e) {
            System.out.println("Error fetching location for IP " + ipAddress + ": " + e.getMessage());
        }

        return "Unknown Location";
    }
}
