package be.steby.CoreProject.dl.enums;

public enum DeviceTrustLevel {
    UNTRUSTED,      // Appareil inconnu ou non vérifié
    BASIC,          // Appareil confirmé une fois
    TRUSTED,        // Appareil utilisé régulièrement
    HIGHLY_TRUSTED  // Appareil principal avec authentification forte
}
