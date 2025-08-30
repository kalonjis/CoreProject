package be.steby.CoreProject.bll.domains.device.utils;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;

/**
 * Utility class for User Agent parsing and device information extraction
 */
public class UserAgentUtils {

    /**
     * Populates device information from HttpServletRequest
     */
    public static void populateDeviceInfo(Device device, UserAgent agent, HttpServletRequest request) {
        // Basic device info
        device.setDeviceType(determineDeviceType(agent));
        device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));
        device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
        device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));

        // Browser version (only if available)
        String browserVersion = agent.getValue(UserAgent.AGENT_VERSION);
        device.setBrowserVersion(browserVersion != null && !browserVersion.isEmpty() ? browserVersion : null);

        // OS version handling
        device.setOsVersion(extractOsVersion(agent));

        // CPU information
        device.setDevice_cpu(agent.getValue(UserAgent.DEVICE_CPU));
        device.setDevice_cpu_bits(agent.getValue(UserAgent.DEVICE_CPU_BITS));

        // Language from Accept-Language header
        device.setLanguage(extractAcceptedLanguage(request));
    }

    /**
     * Populates device information from RequestContext (for async usage)
     */
    public static void populateDeviceInfo(Device device, UserAgent agent, RequestContext context) {
        RequestContext.CapturedDeviceInfo deviceInfo = context.getDeviceInfo();

        if (deviceInfo != null) {
            // Use pre-parsed info from RequestContext
            device.setDeviceType(determineDeviceTypeFromContext(deviceInfo));
            device.setBrowser(deviceInfo.getBrowserName());
            device.setBrowserVersion(deviceInfo.getBrowserVersionMajor());
            device.setOperatingSystem(deviceInfo.getOsName());
            device.setDeviceClass(deviceInfo.getDeviceClass());
            device.setDeviceBrand(deviceInfo.getDeviceBrand());

            // Handle UNDEFINED for osVersion
            String osVersion = deviceInfo.getOsVersionMajor();
            device.setOsVersion(osVersion != null && !"UNDEFINED".equals(osVersion) ? osVersion : "Unknown");
        } else {
            // Fallback to agent parsing
            populateDeviceInfoFromAgent(device, agent, context);
        }

        // Language from headers
        device.setLanguage(normalizeAcceptLanguage(context.getHeaders().getAcceptLanguage()));

        // CPU info not available from RequestContext
        device.setDevice_cpu(null);
        device.setDevice_cpu_bits(null);
    }

    /**
     * Fallback method to populate from agent when RequestContext.DeviceInfo is null
     */
    private static void populateDeviceInfoFromAgent(Device device, UserAgent agent, RequestContext context) {
        device.setDeviceType(determineDeviceType(agent));
        device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));

        String browserVersion = agent.getValue(UserAgent.AGENT_VERSION);
        device.setBrowserVersion(browserVersion != null && !browserVersion.isEmpty() ? browserVersion : null);

        device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        device.setOsVersion(extractOsVersion(agent));
        device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
        device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
    }

    /**
     * Determines device type from UserAgent
     */
    public static String determineDeviceType(UserAgent agent) {
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

        // Fallback to device name analysis
        String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
        if (deviceName != null && !deviceName.isEmpty()) {
            String lowerDeviceName = deviceName.toLowerCase();
            if (lowerDeviceName.contains("mobile") || lowerDeviceName.contains("phone")) {
                return "MOBILE";
            } else if (lowerDeviceName.contains("tablet")) {
                return "TABLET";
            }
        }

        return "UNKNOWN";
    }

    /**
     * Determines device type from RequestContext.CapturedDeviceInfo
     */
    private static String determineDeviceTypeFromContext(RequestContext.CapturedDeviceInfo deviceInfo) {
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
            }
        }

        return "UNKNOWN";
    }

    /**
     * Extracts OS version, handling various edge cases
     */
    public static String extractOsVersion(UserAgent agent) {
        String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);

        // If version is null or empty, try alternative methods
        if (osVersion == null || osVersion.isEmpty() || "??".equals(osVersion)) {
            // Try alternative fields
            osVersion = agent.getValue("OperatingSystemVersionBuild");

            if (osVersion == null || osVersion.isEmpty()) {
                osVersion = agent.getValue("OperatingSystemNameVersion");
                if (osVersion != null && !osVersion.isEmpty()) {
                    // Try to extract just the version part
                    String[] parts = osVersion.split(" ");
                    if (parts.length > 1) {
                        osVersion = parts[parts.length - 1];
                    }
                }
            }
        }

        // Return "Unknown" instead of "UNDEFINED" for consistency
        return osVersion != null && !osVersion.isEmpty() && !"??".equals(osVersion) ? osVersion : "Unknown";
    }

    /**
     * Extracts preferred language from HTTP request
     */
    public static String extractAcceptedLanguage(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        return normalizeAcceptLanguage(acceptLanguage);
    }

    /**
     * Normalizes Accept-Language header
     */
    public static String normalizeAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return "Unknown";
        }

        // Take only the primary language (before comma)
        if (acceptLanguage.contains(",")) {
            acceptLanguage = acceptLanguage.split(",")[0];
        }

        // Remove quality (q=0.9)
        if (acceptLanguage.contains(";")) {
            acceptLanguage = acceptLanguage.split(";")[0];
        }

        return acceptLanguage.trim().toLowerCase();
    }
}