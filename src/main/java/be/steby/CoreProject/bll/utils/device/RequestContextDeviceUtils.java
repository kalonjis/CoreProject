package be.steby.CoreProject.bll.utils.device;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import nl.basjes.parse.useragent.UserAgent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RequestContextDeviceUtils {

    public static String generateFingerprint(RequestContext context, Long userId) {
        StringBuilder fingerprint = new StringBuilder();

        fingerprint.append(context.getUserAgent() != null ? context.getUserAgent() : "unknown");
        fingerprint.append("_").append(context.getClientIp());

        RequestContext.CapturedHeaders headers = context.getHeaders();
        fingerprint.append("_").append(headers.getAcceptLanguage() != null ? headers.getAcceptLanguage() : "");
        fingerprint.append("_").append(userId);

        return fingerprint.toString();
    }

    public static void populateDeviceInfo(Device device, UserAgent agent, RequestContext context) {
        device.setDeviceType(DeviceDetectionUtils.determineDeviceType(agent));
        device.setBrowser(agent.getValue(UserAgent.AGENT_NAME));
        device.setBrowserVersion(agent.getValue(UserAgent.AGENT_VERSION));
        device.setOperatingSystem(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME));
        device.setOsVersion(DeviceDetectionUtils.getOsVersion(agent));
        device.setDevice_cpu(agent.getValue(UserAgent.DEVICE_CPU));
        device.setDevice_cpu_bits(agent.getValue(UserAgent.DEVICE_CPU_BITS));
        device.setLanguage(context.getHeaders().getAcceptLanguage());
        device.setDeviceClass(agent.getValue(UserAgent.DEVICE_CLASS));
        device.setDeviceBrand(agent.getValue(UserAgent.DEVICE_BRAND));
    }

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
            return data.hashCode() + "_FALLBACK";
        }
    }
}
