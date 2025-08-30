package be.steby.CoreProject.bll.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for IP address handling and geolocation resolution
 */
@Slf4j
public class IpLocationUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extracts client IP address from HTTP request, handling proxy headers
     */
    public static String extractClientIp(HttpServletRequest request) {
        // Check for forwarded IP headers (reverse proxy, load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        String xOriginalForwardedFor = request.getHeader("X-Original-Forwarded-For");
        if (xOriginalForwardedFor != null && !xOriginalForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xOriginalForwardedFor)) {
            return xOriginalForwardedFor.split(",")[0].trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Resolves geographic location from IP address using ip-api.com service
     * Returns a formatted location string or "Unknown Location" if resolution fails
     */
    public static String resolveLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() ||
                "127.0.0.1".equals(ipAddress) || "localhost".equals(ipAddress) ||
                ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") ||
                (ipAddress.startsWith("172.") && isPrivateIpRange(ipAddress))) {
            return "Local/Private Network";
        }

        try {
            String apiUrl = "http://ip-api.com/json/" + ipAddress + "?fields=status,country,regionName,city,query";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000);    // 5 seconds

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return parseLocationFromResponse(response.toString());
            } else {
                log.warn("Failed to fetch location for IP {} with response code: {}", ipAddress, responseCode);
            }
        } catch (Exception e) {
            log.warn("Error fetching location for IP {}: {}", ipAddress, e.getMessage());
        }

        return "Unknown Location";
    }

    /**
     * Parses the JSON response from ip-api.com and formats location string
     */
    private static String parseLocationFromResponse(String jsonResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            if (!"success".equals(jsonNode.path("status").asText())) {
                return "Unknown Location";
            }

            String country = jsonNode.path("country").asText();
            String region = jsonNode.path("regionName").asText();
            String city = jsonNode.path("city").asText();

            return formatLocationString(city, region, country);

        } catch (Exception e) {
            log.warn("Error parsing location response: {}", e.getMessage());
            return "Unknown Location";
        }
    }

    /**
     * Formats location components into a readable string
     */
    private static String formatLocationString(String city, String region, String country) {
        StringBuilder location = new StringBuilder();

        if (city != null && !city.isEmpty() && !"null".equals(city)) {
            location.append(city);
        }

        if (region != null && !region.isEmpty() && !"null".equals(region)) {
            if (location.length() > 0) location.append(", ");
            location.append(region);
        }

        if (country != null && !country.isEmpty() && !"null".equals(country)) {
            if (location.length() > 0) location.append(", ");
            location.append(country);
        }

        return location.length() > 0 ? location.toString() : "Unknown Location";
    }

    /**
     * Checks if an IP address is in the private 172.x.x.x range (172.16.0.0 to 172.31.255.255)
     */
    private static boolean isPrivateIpRange(String ipAddress) {
        try {
            String[] parts = ipAddress.split("\\.");
            if (parts.length != 4) return false;

            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if an IP address has a valid format (basic validation)
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        // Basic IPv4 validation
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determines if an IP address is private/local
     */
    public static boolean isPrivateOrLocalIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }

        return "127.0.0.1".equals(ipAddress) ||
                "localhost".equals(ipAddress) ||
                ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
                (ipAddress.startsWith("172.") && isPrivateIpRange(ipAddress));
    }
}