# Quick Start Guide for the Security Logging System

This guide provides a quick introduction to implementing the security logging system in your application. Follow these steps to set up and start using the system quickly.

## Basic Installation

### Step 1: Add Required Dependencies

Ensure you have the necessary dependencies in your `pom.xml` or `build.gradle` file:

```xml
<!-- For Spring AOP support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- For JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### Step 2: Create the Database Schema

Run the provided SQL script to create the `connection_logs` table and indexes:

```sql
CREATE TABLE connection_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id BIGINT,
    ip_address VARCHAR(50),
    location VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    action_type VARCHAR(50) NOT NULL,
    action_details VARCHAR(255),
    session_id VARCHAR(100),
    duration_seconds BIGINT,
    risk_level INTEGER,
    triggered_alert BOOLEAN,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(255),
    
    FOREIGN KEY (user_id) REFERENCES user_(id),
    FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE INDEX idx_log_user_timestamp ON connection_logs(user_id, timestamp);
CREATE INDEX idx_log_action_type ON connection_logs(action_type);
CREATE INDEX idx_log_ip_address ON connection_logs(ip_address);
```

### Step 3: Enable Required Spring Features

Create configuration classes to enable AOP and scheduling:

```java
@Configuration
@EnableAspectJAutoProxy
public class AspectConfig {
}

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

## Core Components

### Step 4: Implement the ActionLogType Enum

Create the enum that defines all possible action types:

```java
public enum ActionLogType {
    AUTH_LOGIN("Connexion utilisateur"),
    AUTH_LOGOUT("Déconnexion utilisateur"),
    AUTH_LOGIN_FAILED("Tentative de connexion échouée"),
    PASSWORD_CHANGED("Modification du mot de passe"),
    // Add all action types here...
    
    private final String description;
    
    ActionLogType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCategory() {
        return this.name().split("_")[0];
    }
}
```

### Step 5: Create the ConnectionLog Entity

Implement the entity for storing log records:

```java
@Entity
@Table(name = "connection_logs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionLog extends BaseEntity<Long> {

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

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "action_details", length = 255)
    private String actionDetails;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "risk_level")
    private Integer riskLevel;

    @Column(name = "triggered_alert")
    private Boolean triggeredAlert;
}