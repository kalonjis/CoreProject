//package be.steby.CoreProject.bll.common.models;
//
//import lombok.Builder;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//
///**
// * Contexte de requête capturé pour une utilisation asynchrone.
// * DTO interne utilisé par les services de la couche business.
// */
//@Getter
//@Builder
//@RequiredArgsConstructor
//public class RequestContext {
//    private final String clientIp;
//    private final String userAgent;
//    private final String sessionId;
//    private final String requestId;
//    private final CapturedHeaders headers;
//    private final Long sessionDurationSeconds;
//    private final Long sessionCreationTime;
//
//    // Nouvelles propriétés pour une reconstruction parfaite du fingerprint
//    private final CapturedDeviceInfo deviceInfo;
//
//    @Getter
//    @Builder
//    public static class CapturedHeaders {
//        private final String xForwardedFor;
//        private final String acceptLanguage;
//        private final String acceptEncoding;
//        private final String accept;
//        private final String referer;
//        private final String dnt;
//        private final String upgradeInsecureRequests;
//
//        // Headers additionnels si fournis par le client
//        private final String xScreenResolution;
//        private final String xTimezone;
//        private final String xPlatform;
//    }
//
//    @Getter
//    @Builder
//    public static class CapturedDeviceInfo {
//        // Infos déjà parsées pour éviter de re-parser le User-Agent
//        private final String osName;
//        private final String osVersionMajor;
//        private final String browserName;
//        private final String browserVersionMajor;
//        private final String deviceClass;
//        private final String deviceName;
//        private final String deviceBrand;
//        private final String deviceType;
//    }
//}