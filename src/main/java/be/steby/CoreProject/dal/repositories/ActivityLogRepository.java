//package be.steby.CoreProject.dal.repositories;
//
//import be.steby.CoreProject.dl.entities.ActivityLog;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.enums.oldActionLogType;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.Instant;
//import java.util.List;
//
//@Repository
//public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
//
//    /**
//     * Trouve tous les logs de connexion pour un utilisateur spécifique
//     */
//    Page<ActivityLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);
//
//    /**
//     * Recherche les logs par type d'action
//     */
//    //Page<ActivityLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);
//
//    /**
//     * Recherche les logs par utilisateur et type d'action
//     */
//    //Page<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(User user, String actionType, Pageable pageable);
//
//    /**
//     * Trouve les derniers logs d'un certain type pour un utilisateur
//     */
//    //List<ActivityLog> findTop10ByUserAndActionTypeOrderByTimestampDesc(User user, String actionType);
//
//    /**
//     * Recherche les logs d'un utilisateur selon plusieurs types d'actions et une période
//     */
////    @Query("SELECT cl FROM ActivityLog cl WHERE cl.user = :user " +
////            "AND (:actionTypes IS NULL OR cl.actionType IN :actionTypes) " +
////            "AND cl.timestamp BETWEEN :startDate AND :endDate " +
////            "ORDER BY cl.timestamp DESC")
////    Page<ActivityLog> findByUserAndActionTypeInAndTimestampBetween(
////            @Param("user") User user,
////            @Param("actionTypes") List<String> actionTypes,
////            @Param("startDate") Instant startDate,
////            @Param("endDate") Instant endDate,
////            Pageable pageable);
//
//    /**
//     * Recherche avancée de logs selon plusieurs critères
//     */
//    @Query("SELECT cl FROM ActivityLog cl WHERE " +
//            "(:userId IS NULL OR cl.user.id = :userId) AND " +
//            "(:ipAddress IS NULL OR cl.ipAddress = :ipAddress) AND " +
//            "(:actionTypes IS NULL OR cl.actionType IN :actionTypes) AND " +
//            "(:successful IS NULL OR cl.successful = :successful) AND " +
//            "cl.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY cl.timestamp DESC")
//    Page<ActivityLog> searchLogs(
//            @Param("userId") Long userId,
//            @Param("ipAddress") String ipAddress,
//            @Param("actionTypes") List<String> actionTypes,
//            @Param("successful") Boolean successful,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate,
//            Pageable pageable);
//
//    /**
//     * Compte les logs par utilisateur, type d'action, statut et période
//     */
//    long countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
//            User user, String actionType, boolean successful, Instant startDate, Instant endDate);
//
//    /**
//     * Compte les logs par type d'action, statut et période
//     */
//    long countByActionTypeAndSuccessfulAndTimestampBetween(
//            String actionType, boolean successful, Instant startDate, Instant endDate);
//
//    /**
//     * Compte les logs par utilisateur, type d'action et période
//     */
//    long countByUserAndActionTypeAndTimestampBetween(
//            User user, String actionType, Instant startDate, Instant endDate);
//
//    /**
//     * Vérifie si un utilisateur s'est connecté depuis une IP dans une période donnée
//     */
//    boolean existsByUserAndIpAddressAndTimestampAfter(
//            User user, String ipAddress, Instant since);
//
//    /**
//     * Vérifie si un utilisateur s'est connecté depuis une localisation dans une période donnée
//     */
//    boolean existsByUserAndLocationAndTimestampAfter(
//            User user, String location, Instant since);
//
//    /**
//     * Compte le nombre d'appareils distincts utilisés par un utilisateur dans une période
//     */
//    @Query("SELECT COUNT(DISTINCT cl.device.id) FROM ActivityLog cl WHERE cl.user.id = :userId " +
//            "AND cl.device IS NOT NULL AND cl.timestamp BETWEEN :startDate AND :endDate")
//    Long countDistinctDevicesByUserAndTimestampBetween(
//            @Param("userId") Long userId,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Trouve les adresses IP distinctes utilisées par un utilisateur dans une période
//     */
//    @Query("SELECT DISTINCT cl.ipAddress FROM ActivityLog cl WHERE cl.user = :user " +
//            "AND cl.timestamp BETWEEN :startDate AND :endDate")
//    List<String> findDistinctIpAddressesByUserAndTimestampBetween(
//            @Param("user") User user,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Compte le nombre d'utilisateurs uniques pour un type d'action dans une période
//     */
//    @Query("SELECT COUNT(DISTINCT cl.user.id) FROM ActivityLog cl WHERE cl.actionType = :actionType " +
//            "AND cl.timestamp BETWEEN :startDate AND :endDate")
//    Long countDistinctUsersByActionTypeAndTimestampBetween(
//            @Param("actionType") String actionType,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Compte le nombre d'adresses IP uniques pour un type d'action dans une période
//     */
//    @Query("SELECT COUNT(DISTINCT cl.ipAddress) FROM ActivityLog cl WHERE cl.actionType = :actionType " +
//            "AND cl.timestamp BETWEEN :startDate AND :endDate")
//    Long countDistinctIpAddressesByActionTypeAndTimestampBetween(
//            @Param("actionType") String actionType,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Compte les connexions par localisation
//     */
//    @Query("SELECT cl.location, COUNT(cl.id) FROM ActivityLog cl WHERE cl.actionType = 'AUTH_LOGIN' " +
//            "AND cl.successful = true AND cl.timestamp BETWEEN :startDate AND :endDate " +
//            "GROUP BY cl.location ORDER BY COUNT(cl.id) DESC")
//    List<Object[]> countLoginsByLocation(
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Compte les connexions par jour
//     */
//    @Query(value = "SELECT CAST(timestamp as date) as login_date, COUNT(*) " +
//            "FROM connection_logs " +
//            "WHERE action_type = 'AUTH_LOGIN' AND successful = true " +
//            "AND timestamp BETWEEN :startDate AND :endDate " +
//            "GROUP BY CAST(timestamp as date) " +
//            "ORDER BY login_date", nativeQuery = true)
//    List<Object[]> countLoginsByDay(
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Recherche des activités suspectes: connexions depuis différentes adresses IP
//     * dans un court intervalle de temps
//     */
//    @Query("SELECT cl FROM ActivityLog cl WHERE cl.user = :user AND cl.actionType = 'AUTH_LOGIN' " +
//            "AND cl.successful = true AND cl.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY cl.timestamp DESC")
//    List<ActivityLog> findSuspiciousActivities(
//            @Param("user") User user,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//
//    /**
//     * Recherche les logs par type d'action (ENUM) - TYPE SAFE
//     */
//    Page<ActivityLog> findByActionTypeOrderByTimestampDesc(oldActionLogType actionType, Pageable pageable);
//
//    /**
//     * Recherche les logs par utilisateur et type d'action (ENUM) - TYPE SAFE
//     */
//    Page<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(User user, oldActionLogType actionType, Pageable pageable);
//
//    /**
//     * Trouve les derniers logs d'un certain type pour un utilisateur (ENUM) - TYPE SAFE
//     */
//    List<ActivityLog> findTop10ByUserAndActionTypeOrderByTimestampDesc(User user, oldActionLogType actionType);
//
//    /**
//     * Recherche par types d'actions multiples (ENUM) - TYPE SAFE
//     */
//    @Query("SELECT al FROM ActivityLog al WHERE al.actionType IN :actionTypes ORDER BY al.timestamp DESC")
//    Page<ActivityLog> findByActionTypeInOrderByTimestampDesc(
//            @Param("actionTypes") List<oldActionLogType> actionTypes,
//            Pageable pageable);
//
//    /**
//     * Recherche par types d'actions avec période (ENUM) - TYPE SAFE
//     */
//    @Query("SELECT al FROM ActivityLog al WHERE " +
//            "al.actionType IN :actionTypes AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY al.timestamp DESC")
//    Page<ActivityLog> findByActionTypeInAndTimestampBetweenOrderByTimestampDesc(
//            @Param("actionTypes") List<oldActionLogType> actionTypes,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate,
//            Pageable pageable);
//
//    /**
//     * Recherche par utilisateur et types d'actions (ENUM) - TYPE SAFE
//     */
//    @Query("SELECT al FROM ActivityLog al WHERE " +
//            "al.user = :user AND " +
//            "(:actionTypes IS NULL OR al.actionType IN :actionTypes) AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY al.timestamp DESC")
//    Page<ActivityLog> findByUserAndActionTypeInAndTimestampBetween(
//            @Param("user") User user,
//            @Param("actionTypes") List<oldActionLogType> actionTypes,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate,
//            Pageable pageable);
//
//    /**
//     * Recherche avancée avec enum pour actionTypes - TYPE SAFE
//     */
//    @Query("SELECT al FROM ActivityLog al WHERE " +
//            "(:userId IS NULL OR al.user.id = :userId) AND " +
//            "(:ipAddress IS NULL OR al.ipAddress = :ipAddress) AND " +
//            "(:actionTypes IS NULL OR al.actionType IN :actionTypes) AND " +
//            "(:successful IS NULL OR al.successful = :successful) AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY al.timestamp DESC")
//    Page<ActivityLog> searchLogsWithEnums(
//            @Param("userId") Long userId,
//            @Param("ipAddress") String ipAddress,
//            @Param("actionTypes") List<oldActionLogType> actionTypes,
//            @Param("successful") Boolean successful,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate,
//            Pageable pageable);
//
//    /**
//     * Compte par type d'action avec enum - TYPE SAFE
//     */
//    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE " +
//            "al.actionType = :actionType AND " +
//            "al.successful = :successful AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate")
//    long countByActionTypeAndSuccessfulAndTimestampBetween(
//            @Param("actionType") oldActionLogType actionType,
//            @Param("successful") boolean successful,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Compte par utilisateur et type d'action avec enum - TYPE SAFE
//     */
//    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE " +
//            "al.user = :user AND " +
//            "al.actionType = :actionType AND " +
//            "al.successful = :successful AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate")
//    long countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
//            @Param("user") User user,
//            @Param("actionType") oldActionLogType actionType,
//            @Param("successful") boolean successful,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Trouve le dernier log d'un certain type pour un utilisateur avec statut - TYPE SAFE
//     */
//    ActivityLog findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
//            User user, oldActionLogType actionType, boolean successful);
//
//    /**
//     * Statistiques par catégorie d'actions - TYPE SAFE
//     */
//    @Query("SELECT al.actionType, COUNT(al) FROM ActivityLog al WHERE " +
//            "al.timestamp BETWEEN :startDate AND :endDate " +
//            "GROUP BY al.actionType " +
//            "ORDER BY COUNT(al) DESC")
//    List<Object[]> getActionTypeStatistics(
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate);
//
//    /**
//     * Recherche les actions par catégorie - TYPE SAFE
//     */
//    @Query("SELECT al FROM ActivityLog al WHERE " +
//            "SUBSTRING(al.actionType, 1, LOCATE('_', al.actionType) - 1) = :category AND " +
//            "al.timestamp BETWEEN :startDate AND :endDate " +
//            "ORDER BY al.timestamp DESC")
//    Page<ActivityLog> findByActionCategoryAndTimestampBetween(
//            @Param("category") String category,
//            @Param("startDate") Instant startDate,
//            @Param("endDate") Instant endDate,
//            Pageable pageable);
//
//// ================== MÉTHODES DE MIGRATION (à supprimer plus tard) ==================
//
//    /**
//     * @deprecated Utiliser findByActionTypeOrderByTimestampDesc(ActionLogType, Pageable) à la place
//     */
//    @Deprecated
//    Page<ActivityLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);
//
//    /**
//     * @deprecated Utiliser findByUserAndActionTypeOrderByTimestampDesc(User, ActionLogType, Pageable) à la place
//     */
//    @Deprecated
//    Page<ActivityLog> findByUserAndActionTypeOrderByTimestampDesc(User user, String actionType, Pageable pageable);
//
//    /**
//     * @deprecated Utiliser findTop10ByUserAndActionTypeOrderByTimestampDesc(User, ActionLogType) à la place
//     */
//    @Deprecated
//    List<ActivityLog> findTop10ByUserAndActionTypeOrderByTimestampDesc(User user, String actionType);
//
//    /**
//     * Trouve le dernier log d'un certain type pour un utilisateur avec un statut spécifique
//     */
//    ActivityLog findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
//            User user, String actionType, boolean successful);
//
//
//    /**
//     * Supprime les logs antérieurs à une date donnée
//     */
//    @Modifying
//    @Query("DELETE FROM ActivityLog cl WHERE cl.timestamp < :cutoffDate")
//    long deleteByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);
//
//
//    /**
//     * Trouve tous les logs par utilisateur et types d'actions spécifiés
//     */
//    Page<ActivityLog> findByUserAndActionTypeInOrderByTimestampDesc(
//            User user,
//            List<String> actionTypes,
//            Pageable pageable);
//
//    /**
//     * Trouve les N derniers logs d'un utilisateur pour des types d'actions spécifiés
//     */
//    @Query("SELECT cl FROM ActivityLog cl WHERE cl.user = :user " +
//            "AND cl.actionType IN :actionTypes " +
//            "ORDER BY cl.timestamp DESC")
//    List<ActivityLog> findTopNByUserAndActionTypeInOrderByTimestampDesc(
//            @Param("user") User user,
//            @Param("actionTypes") List<String> actionTypes,
//            Pageable pageable);
//
//    /**
//     * Compte les logs par utilisateur, type d'action, statut et période (avec statut optionnel)
//     */
//    long countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
//            User user,
//            String actionType,
//            boolean successful,
//            Instant timestamp);
//}