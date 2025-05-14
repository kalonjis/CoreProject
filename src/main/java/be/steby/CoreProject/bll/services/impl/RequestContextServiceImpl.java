package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

/**
 * Service pour extraire et capturer les informations de la requête HTTP
 * avant qu'elle ne soit recyclée par Spring.
 * Utilisé pour l'exécution asynchrone où l'accès direct à la requête n'est plus possible.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestContextServiceImpl implements RequestContextService {

    private final UserAgentAnalyzer userAgentAnalyzer;

    /**
     * Capture les informations essentielles de la requête HTTP
     */
    @Override
    public RequestContext captureRequestContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String sessionId = null;
        Long sessionCreationTime = null;
        Long sessionDurationSeconds = null;

        if (session != null) {
            sessionId = session.getId();
            sessionCreationTime = session.getCreationTime();
            // Calculer la durée de session au moment de la capture
            long now = System.currentTimeMillis();
            sessionDurationSeconds = (now - sessionCreationTime) / 1000;
        }

        // Parser le User-Agent pour capturer les infos device
        RequestContext.CapturedDeviceInfo deviceInfo = null;
        String userAgentString = request.getHeader("User-Agent");
        if (userAgentString != null) {
            UserAgent agent = userAgentAnalyzer.parse(userAgentString);
            deviceInfo = RequestContext.CapturedDeviceInfo.builder()
                    .osName(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME))
                    .osVersionMajor(agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR))
                    .browserName(agent.getValue(UserAgent.AGENT_NAME))
                    .browserVersionMajor(agent.getValue(UserAgent.AGENT_VERSION_MAJOR))
                    .deviceClass(agent.getValue(UserAgent.DEVICE_CLASS))
                    .deviceName(agent.getValue(UserAgent.DEVICE_NAME))
                    .deviceBrand(agent.getValue(UserAgent.DEVICE_BRAND))
                    .deviceType(agent.getValue("DeviceType"))
                    .build();
        }

        return RequestContext.builder()
                .clientIp(extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .sessionId(sessionId)
                .requestId(request.getRequestId())
                .sessionCreationTime(sessionCreationTime)
                .sessionDurationSeconds(sessionDurationSeconds)
                .deviceInfo(deviceInfo)
                .headers(RequestContext.CapturedHeaders.builder()
                        .xForwardedFor(request.getHeader("X-Forwarded-For"))
                        .acceptLanguage(request.getHeader("Accept-Language"))
                        .acceptEncoding(request.getHeader("Accept-Encoding"))
                        .accept(request.getHeader("Accept"))
                        .referer(request.getHeader("Referer"))
                        .dnt(request.getHeader("DNT"))
                        .upgradeInsecureRequests(request.getHeader("Upgrade-Insecure-Requests"))
                        // Headers additionnels pour le fingerprint
                        .xScreenResolution(request.getHeader("X-Screen-Resolution"))
                        .xTimezone(request.getHeader("X-Timezone"))
                        .xPlatform(request.getHeader("X-Platform"))
                        .build())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}