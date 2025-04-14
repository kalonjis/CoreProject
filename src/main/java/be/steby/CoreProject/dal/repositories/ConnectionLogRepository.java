package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ConnectionLogRepository extends JpaRepository<ConnectionLog, Long> {

    /**
     * Trouve tous les logs de connexion pour un utilisateur spécifique
     */
    Page<ConnectionLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    /**
     * Recherche les logs par type d'action
     */
    Page<ConnectionLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);

    /**
     * Recherche les logs par utilisateur et type d'action
     */
    Page<ConnectionLog> findByUserAndActionTypeOrderByTimestampDesc(User user, String actionType, Pageable pageable);

    /**
     * Trouve les derniers logs d'un certain type pour un utilisateur
     */
    List<ConnectionLog> findTop10ByUserAndActionTypeOrderByTimestampDesc(User user, String actionType);

    /**
     * Recherche les logs d'un utilisateur selon plusieurs types d'actions et une période
     */
    @Query("SELECT cl FROM ConnectionLog cl WHERE cl.user = :user " +
            "AND (:actionTypes IS NULL OR cl.actionType IN :actionTypes) " +
            "AND cl.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY cl.timestamp DESC")
    Page<ConnectionLog> findByUserAndActionTypeInAndTimestampBetween(
            @Param("user") User user,
            @Param("actionTypes") List<String> actionTypes,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Recherche avancée de logs selon plusieurs critères
     */
    @Query("SELECT cl FROM ConnectionLog cl WHERE " +
            "(:userId IS NULL OR cl.user.id = :userId) AND " +
            "(:ipAddress IS NULL OR cl.ipAddress = :ipAddress) AND " +
            "(:actionTypes IS NULL OR cl.actionType IN :actionTypes) AND " +
            "(:successful IS NULL OR cl.successful = :successful) AND " +
            "cl.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY cl.timestamp DESC")
    Page<ConnectionLog> searchLogs(
            @Param("userId") Long userId,
            @Param("ipAddress") String ipAddress,
            @Param("actionTypes") List<String> actionTypes,
            @Param("successful") Boolean successful,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Compte les logs par utilisateur, type d'action, statut et période
     */
    long countByUserAndActionTypeAndSuccessfulAndTimestampBetween(
            User user, String actionType, boolean successful, Instant startDate, Instant endDate);

    /**
     * Compte les logs par type d'action, statut et période
     */
    long countByActionTypeAndSuccessfulAndTimestampBetween(
            String actionType, boolean successful, Instant startDate, Instant endDate);

    /**
     * Compte les logs par utilisateur, type d'action et période
     */
    long countByUserAndActionTypeAndTimestampBetween(
            User user, String actionType, Instant startDate, Instant endDate);

    /**
     * Vérifie si un utilisateur s'est connecté depuis une IP dans une période donnée
     */
    boolean existsByUserAndIpAddressAndTimestampAfter(
            User user, String ipAddress, Instant since);

    /**
     * Vérifie si un utilisateur s'est connecté depuis une localisation dans une période donnée
     */
    boolean existsByUserAndLocationAndTimestampAfter(
            User user, String location, Instant since);

    /**
     * Compte le nombre d'appareils distincts utilisés par un utilisateur dans une période
     */
    @Query("SELECT COUNT(DISTINCT cl.device.id) FROM ConnectionLog cl WHERE cl.user.id = :userId " +
            "AND cl.device IS NOT NULL AND cl.timestamp BETWEEN :startDate AND :endDate")
    Long countDistinctDevicesByUserAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Trouve les adresses IP distinctes utilisées par un utilisateur dans une période
     */
    @Query("SELECT DISTINCT cl.ipAddress FROM ConnectionLog cl WHERE cl.user = :user " +
            "AND cl.timestamp BETWEEN :startDate AND :endDate")
    List<String> findDistinctIpAddressesByUserAndTimestampBetween(
            @Param("user") User user,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Compte le nombre d'utilisateurs uniques pour un type d'action dans une période
     */
    @Query("SELECT COUNT(DISTINCT cl.user.id) FROM ConnectionLog cl WHERE cl.actionType = :actionType " +
            "AND cl.timestamp BETWEEN :startDate AND :endDate")
    Long countDistinctUsersByActionTypeAndTimestampBetween(
            @Param("actionType") String actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Compte le nombre d'adresses IP uniques pour un type d'action dans une période
     */
    @Query("SELECT COUNT(DISTINCT cl.ipAddress) FROM ConnectionLog cl WHERE cl.actionType = :actionType " +
            "AND cl.timestamp BETWEEN :startDate AND :endDate")
    Long countDistinctIpAddressesByActionTypeAndTimestampBetween(
            @Param("actionType") String actionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Compte les connexions par localisation
     */
    @Query("SELECT cl.location, COUNT(cl.id) FROM ConnectionLog cl WHERE cl.actionType = 'AUTH_LOGIN' " +
            "AND cl.successful = true AND cl.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY cl.location ORDER BY COUNT(cl.id) DESC")
    List<Object[]> countLoginsByLocation(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Compte les connexions par jour
     */
    @Query(value = "SELECT CAST(timestamp as date) as login_date, COUNT(*) " +
            "FROM connection_logs " +
            "WHERE action_type = 'AUTH_LOGIN' AND successful = true " +
            "AND timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(timestamp as date) " +
            "ORDER BY login_date", nativeQuery = true)
    List<Object[]> countLoginsByDay(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Recherche des activités suspectes: connexions depuis différentes adresses IP
     * dans un court intervalle de temps
     */
    @Query("SELECT cl FROM ConnectionLog cl WHERE cl.user = :user AND cl.actionType = 'AUTH_LOGIN' " +
            "AND cl.successful = true AND cl.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY cl.timestamp DESC")
    List<ConnectionLog> findSuspiciousActivities(
            @Param("user") User user,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);


    /**
     * Trouve le dernier log d'un certain type pour un utilisateur avec un statut spécifique
     */
    ConnectionLog findTopByUserAndActionTypeAndSuccessfulOrderByTimestampDesc(
            User user, String actionType, boolean successful);


    /**
     * Supprime les logs antérieurs à une date donnée
     */
    @Modifying
    @Query("DELETE FROM ConnectionLog cl WHERE cl.timestamp < :cutoffDate")
    long deleteByTimestampBefore(@Param("cutoffDate") Instant cutoffDate);
}