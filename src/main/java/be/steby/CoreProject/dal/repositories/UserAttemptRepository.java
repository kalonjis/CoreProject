package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAttempt;
import be.steby.CoreProject.dl.enums.AttemptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAttemptRepository extends JpaRepository<UserAttempt, Long> {

    Optional<UserAttempt> findByUserAndAttemptType(User user, AttemptType attemptType);

    Optional<UserAttempt> findByUserAndAttemptTypeAndDeviceId(User user, AttemptType attemptType, Long deviceId);
}
