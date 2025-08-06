package be.steby.CoreProject.dl.enums;

public enum DeactivationReason {
    TAKING_A_BREAK("Prendre une pause"),
    TOO_MUCH_TIME("Trop de temps passé sur l'application"),
    PRIVACY_CONCERNS("Préoccupations de confidentialité"),
    ACCOUNT_CLEANUP("Nettoyage de mes comptes"),
    SWITCHING_ACCOUNTS("Changement vers un autre compte"),
    WORK_REQUIREMENTS("Exigences professionnelles"),
    NOT_USEFUL("L'application ne m'est plus utile"),
    GDPR_REQUEST("Suppression conformément au GDPR"),
    ADMIN_DECISION("Suppression par administrateur"),
    OTHER("Autre raison");

    private final String displayName;

    DeactivationReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
