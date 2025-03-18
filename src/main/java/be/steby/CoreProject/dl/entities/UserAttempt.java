package be.steby.CoreProject.dl.entities;


import be.steby.CoreProject.dl.enums.AttemptType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_attempt", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "attempt_type", "device_id"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAttempt extends BaseEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptType attemptType;


    @Column(name = "device_id")
    private Long deviceId;

    @Column(nullable = false)
    private int attemptCount;

    private Instant lastAttemptTime;


    public UserAttempt(User user, AttemptType attemptType){
        this.user = user;
        this.attemptType = attemptType;
        this.deviceId = null;
        this.attemptCount = 0;
    }


    public UserAttempt(User user, AttemptType attemptType, Long deviceId){
        this.user = user;
        this.attemptType = attemptType;
        this.deviceId = deviceId;
        this.attemptCount = 0;
    }
}
