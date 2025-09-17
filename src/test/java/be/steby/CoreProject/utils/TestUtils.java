// src/test/java/be/steby/CoreProject/utils/TestUtils.java
package be.steby.CoreProject.utils;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Classe utilitaire pour les tests.
 * Centralise les méthodes communes utilisées dans plusieurs tests.
 */
public class TestUtils {

    /**
     * Définit l'ID d'une entité pour les tests.
     * Utilise ReflectionTestUtils de Spring pour plus de sécurité.
     */
    public static void setEntityId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    /**
     * Méthode spécialisée pour les Device.
     */
    public static void setDeviceId(Device device, Long id) {
        setEntityId(device, id);
    }

    /**
     * Méthode spécialisée pour les User.
     */
    public static void setUserId(User user, Long id) {
        setEntityId(user, id);
    }
}

// Usage dans tes tests :
// TestUtils.setDeviceId(device, 123L);