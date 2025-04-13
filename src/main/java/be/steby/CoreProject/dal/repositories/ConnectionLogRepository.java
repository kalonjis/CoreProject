package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ConnectionLogRepository extends JpaRepository<ConnectionLog, Long> {

    /**
     * Trouver tous les logs de connexion pour un utilisateur spécifique
     */
    Page<ConnectionLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    /**
     * Trouver les tentatives de connexion échouées récentes pour un utilisateur
     */
    List<ConnectionLog> findByUserAndSuccessfulFalseAndTimestampAfter(
            User user, Instant since);

    /**
     * Trouver les dernières connexions réussies pour un utilisateur
     */
    List<ConnectionLog> findTop10ByUserAndSuccessfulTrueOrderByTimestampDesc(User user);

    /**
     * Supprimer les logs antérieurs à une date donnée
     */
    void deleteByTimestampBefore(Instant cutoffDate);

    /**
     * Compter le nombre de tentatives de connexion échouées dans un intervalle de temps
     */
    long countByUserAndSuccessfulFalseAndTimestampBetween(
            User user, Instant start, Instant end);

    /**
     * Recherche des activités suspectes: connexions réussies depuis différentes adresses IP
     * dans un court intervalle de temps
     */
    @Query("SELECT cl FROM ConnectionLog cl WHERE cl.user = ?1 AND cl.successful = true " +
            "AND cl.timestamp BETWEEN ?2 AND ?3 GROUP BY cl.ipAddress HAVING COUNT(cl.ipAddress) > 1")
    List<ConnectionLog> findSuspiciousActivities(User user, Instant start, Instant end);
}