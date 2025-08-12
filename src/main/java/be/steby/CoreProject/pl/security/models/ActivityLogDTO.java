package be.steby.CoreProject.pl.security.models;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.enums.ActionLogType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record ActivityLogDTO(
        Long id,
        Long userId,
        String username,
        Long deviceId,
        String deviceInfo,
        ActionLogType actionType,
        String actionCategory,
        String actionDescription,
        Instant timestamp,
        String formattedTimestamp,
        String ipAddress,
        String location,
        boolean successful,
        String failureReason,
        String actionDetails,
        Integer riskLevel,
        String sessionId,
        Long durationSeconds,
        String formattedDuration,
        Object metadataObj
) {
    public static ActivityLogDTO fromEntity(ActivityLog log) {
        // Extraire les informations de l'appareil
        String deviceInfo = log.getDevice() != null ?
                String.format("%s - %s %s",
                        log.getDevice().getDeviceType(),
                        log.getDevice().getBrowser(),
                        log.getDevice().getOperatingSystem()) : "Inconnu";

        // Formater l'horodatage
        ZonedDateTime zonedDateTime = log.getTimestamp().atZone(ZoneId.systemDefault());
        String formattedTimestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(zonedDateTime);

        // Convertir la durée en format lisible
        String formattedDuration = formatDuration(log.getDurationSeconds());

        // Essayer de convertir l'actionType en enum
        ActionLogType actionType = log.getActionType() != null ? log.getActionType() : null ;

        // Obtenir la catégorie et la description
        String actionCategory = actionType != null ? actionType.getCategory() : "UNKNOWN";
        String actionDescription = actionType != null ? actionType.getDescription() : actionType.name();

        // Traiter les métadonnées si présentes
        Object metadataObj = null;
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            try {
                // Dans un vrai système, vous pourriez utiliser ObjectMapper pour convertir en Object
                // Pour simplifier, on laisse comme une chaîne ici
                metadataObj = log.getMetadata();
            } catch (Exception e) {
                // Ignorer les erreurs de parsing
            }
        }

        return new ActivityLogDTO(
                log.getId(),
                log.getUser().getId(),
                log.getUser().getUsername(),
                log.getDevice() != null ? log.getDevice().getId() : null,
                deviceInfo,
                log.getActionType(),
                actionCategory,
                actionDescription,
                log.getTimestamp(),
                formattedTimestamp,
                log.getIpAddress(),
                log.getLocation(),
                log.isSuccessful(),
                log.getFailureReason(),
                log.getActionDetails(),
                log.getRiskLevel(),
                log.getSessionId(),
                log.getDurationSeconds(),
                formattedDuration,
                metadataObj
        );
    }

    /**
     * Formate une durée en secondes en chaîne lisible (HH:MM:SS ou JJ:HH:MM:SS)
     */
    private static String formatDuration(Long durationSeconds) {
        if (durationSeconds == null) {
            return null;
        }

        long seconds = durationSeconds % 60;
        long minutes = (durationSeconds / 60) % 60;
        long hours = (durationSeconds / (60 * 60)) % 24;
        long days = durationSeconds / (60 * 60 * 24);

        if (days > 0) {
            return String.format("%dj %02dh %02dm %02ds", days, hours, minutes, seconds);
        } else {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        }
    }
}