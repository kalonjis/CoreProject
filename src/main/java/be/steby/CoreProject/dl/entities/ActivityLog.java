package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.ActionLogType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_log_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type"),
        @Index(name = "idx_log_ip_address", columnList = "ip_address")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    // Nouveau champ pour inclure des métadonnées supplémentaires au format JSON
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * ✅ TYPE D'ACTION AVEC ENUM - TYPE SAFE !
     * Utilise l'enum ActionLogType directement avec mapping STRING
     * Hibernate stocke la valeur de l'enum comme string en base
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50, nullable = false)
    private ActionLogType actionType;  // ← ENUM au lieu de String !

    /**
     * Détails complémentaires sur l'action
     */
    @Column(name = "action_details", length = 255)
    private String actionDetails;

    /**
     * Identifiant de session unique pour lier des actions entre elles
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Durée en secondes (pour les sessions ou opérations)
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * Niveau de risque évalué de l'action
     * 0 = normal, 1 = faible, 2 = moyen, 3 = élevé
     */
    @Column(name = "risk_level")
    private Integer riskLevel;

    /**
     * Indique si l'action a déclenché une alerte de sécurité
     */
    @Column(name = "triggered_alert")
    private Boolean triggeredAlert;

    // ✅ Méthodes helper pour compatibilité avec l'ancien code

    /**
     * Obtient la catégorie de l'action.
     * @return La catégorie (AUTH, ACCOUNT, etc.)
     */
    public String getActionCategory() {
        return actionType != null ? actionType.getCategory() : "UNKNOWN";
    }

    /**
     * Obtient la description lisible de l'action.
     * @return Description de l'action
     */
    public String getActionDescription() {
        return actionType != null ? actionType.getDescription() : "Action inconnue";
    }

    /**
     * Vérifie si l'action appartient à une catégorie donnée.
     * @param category La catégorie à vérifier
     * @return true si l'action appartient à cette catégorie
     */
    public boolean isActionCategory(String category) {
        return actionType != null && actionType.isCategory(category);
    }

    /**
     * Getter pour compatibilité avec l'ancien code qui attend un String.
     * @deprecated Utiliser directement getActionType() qui retourne l'enum
     */
    @Deprecated
    public String getActionTypeAsString() {
        return actionType != null ? actionType.name() : null;
    }
}