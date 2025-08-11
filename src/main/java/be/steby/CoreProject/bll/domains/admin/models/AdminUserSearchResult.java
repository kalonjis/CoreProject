package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Représente le résultat d'une recherche d'utilisateurs par un administrateur.
 * Ce modèle encapsule les résultats de la recherche ainsi que des statistiques
 * et métadonnées utiles pour l'interface d'administration.
 */
public record AdminUserSearchResult(
        /**
         * Page de résultats d'utilisateurs.
         */
        Page<User> users,

        /**
         * Critères de recherche utilisés.
         */
        UserSearchCriteria searchCriteria,

        /**
         * Statistiques sur les résultats de la recherche.
         */
        SearchStatistics statistics,

        /**
         * Métadonnées additionnelles sur la recherche.
         */
        Map<String, Object> metadata,

        /**
         * Timestamp de la recherche.
         */
        Instant searchTimestamp
) {

    /**
     * Statistiques sur les résultats de recherche.
     */
    public record SearchStatistics(
            /**
             * Nombre total d'utilisateurs trouvés.
             */
            long totalUsers,

            /**
             * Nombre d'utilisateurs activés.
             */
            long activatedUsers,

            /**
             * Nombre d'utilisateurs désactivés.
             */
            long deactivatedUsers,

            /**
             * Distribution des rôles dans les résultats.
             */
            Map<UserRole, Long> roleDistribution,

            /**
             * Durée de la recherche en millisecondes.
             */
            long searchDurationMs
    ) {

        /**
         * Calcule le pourcentage d'utilisateurs activés.
         *
         * @return Pourcentage d'activation (0-100)
         */
        public double getActivationPercentage() {
            return totalUsers > 0 ? (double) activatedUsers / totalUsers * 100 : 0;
        }

        /**
         * Obtient le nombre d'utilisateurs pour un rôle spécifique.
         *
         * @param role Le rôle recherché
         * @return Nombre d'utilisateurs avec ce rôle
         */
        public long getUserCountForRole(UserRole role) {
            return roleDistribution.getOrDefault(role, 0L);
        }

        /**
         * Vérifie si des utilisateurs avec des rôles administratifs sont présents.
         *
         * @return true si des admins/super-admins sont dans les résultats
         */
        public boolean hasAdministrativeUsers() {
            return getUserCountForRole(UserRole.ADMIN) > 0 ||
                    getUserCountForRole(UserRole.SUPER_ADMIN) > 0;
        }
    }

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminUserSearchResult(
            Page<User> users,
            UserSearchCriteria searchCriteria,
            SearchStatistics statistics,
            Map<String, Object> metadata) {
        this(users, searchCriteria, statistics, metadata, Instant.now());
    }

    /**
     * Crée un résultat de recherche simple avec statistiques de base.
     *
     * @param users Page de résultats
     * @param searchCriteria Critères utilisés
     * @param searchDurationMs Durée de la recherche
     * @return Nouveau résultat de recherche
     */
    public static AdminUserSearchResult simple(
            Page<User> users,
            UserSearchCriteria searchCriteria,
            long searchDurationMs) {

        // Calcul des statistiques de base
        long totalUsers = users.getTotalElements();
        long activatedUsers = users.getContent().stream()
                .mapToLong(user -> user.isEnabled() ? 1 : 0)
                .sum();
        long deactivatedUsers = totalUsers - activatedUsers;

        // Distribution des rôles
        Map<UserRole, Long> roleDistribution = users.getContent().stream()
                .flatMap(user -> user.getUserRoles().stream())
                .collect(java.util.stream.Collectors.groupingBy(
                        role -> role,
                        java.util.stream.Collectors.counting()
                ));

        SearchStatistics statistics = new SearchStatistics(
                totalUsers,
                activatedUsers,
                deactivatedUsers,
                roleDistribution,
                searchDurationMs
        );

        return new AdminUserSearchResult(
                users,
                searchCriteria,
                statistics,
                Map.of("searchType", "simple")
        );
    }

    /**
     * Crée un résultat de recherche avec métadonnées personnalisées.
     *
     * @param users Page de résultats
     * @param searchCriteria Critères utilisés
     * @param statistics Statistiques détaillées
     * @param additionalMetadata Métadonnées supplémentaires
     * @return Nouveau résultat de recherche
     */
    public static AdminUserSearchResult withMetadata(
            Page<User> users,
            UserSearchCriteria searchCriteria,
            SearchStatistics statistics,
            Map<String, Object> additionalMetadata) {

        return new AdminUserSearchResult(
                users,
                searchCriteria,
                statistics,
                additionalMetadata
        );
    }

    /**
     * Vérifie si la recherche a retourné des résultats.
     *
     * @return true si des utilisateurs ont été trouvés
     */
    public boolean hasResults() {
        return users != null && users.hasContent();
    }

    /**
     * Obtient le nombre total d'utilisateurs trouvés.
     *
     * @return Nombre total d'utilisateurs
     */
    public long getTotalUsers() {
        return users != null ? users.getTotalElements() : 0;
    }

    /**
     * Vérifie si la recherche a pris beaucoup de temps.
     *
     * @param thresholdMs Seuil en millisecondes
     * @return true si la recherche a dépassé le seuil
     */
    public boolean isSlowSearch(long thresholdMs) {
        return statistics.searchDurationMs() > thresholdMs;
    }

    /**
     * Obtient une métadonnée spécifique.
     *
     * @param key Clé de la métadonnée
     * @param <T> Type attendu
     * @return Valeur de la métadonnée ou null
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return metadata != null ? (T) metadata.get(key) : null;
    }

    /**
     * Crée un résumé textuel des résultats de recherche.
     *
     * @return Description des résultats
     */
    public String getSummary() {
        if (!hasResults()) {
            return "Aucun utilisateur trouvé";
        }

        String base = String.format(
                "%d utilisateur(s) trouvé(s) (%d activé(s), %d désactivé(s))",
                statistics.totalUsers(),
                statistics.activatedUsers(),
                statistics.deactivatedUsers()
        );

        if (statistics.hasAdministrativeUsers()) {
            base += " - Incluant des utilisateurs administratifs";
        }

        return base;
    }
}