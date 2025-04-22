# Security Logging System Implementation Guide

This guide provides detailed implementation instructions for integrating the security logging system into your Spring Boot application.

## Table of Contents

- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Best Practices](#best-practices)
- [Example Implementations](#example-implementations)
- [Troubleshooting](#troubleshooting)
- [Performance Considerations](#performance-considerations)

## System Requirements

- Java 17 or higher
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL (or compatible database)
- Spring AOP

## Installation

### Database Setup

1. Create the necessary tables using the provided schema:

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

-- Create necessary indexes
CREATE INDEX idx_log_user_timestamp ON connection_logs(user_id, timestamp);
CREATE INDEX idx_log_action_type ON connection_logs(action_type);
CREATE INDEX idx_log_ip_address ON connection_logs(ip_address);
```

### Required Dependencies

Add the following dependencies to your project:

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

<!-- For IP location services (adjust as needed) -->
<dependency>
    <groupId>com.maxmind.geoip2</groupId>
    <artifactId>geoip2</artifactId>
    <version>4.0.1</version>
</dependency>
```

## Configuration

### Enable Aspects

Make sure AOP is enabled in your Spring configuration:

```java
@Configuration
@EnableAspectJAutoProxy
public class AspectConfig {
}
```

### Configure Scheduled Tasks

For log cleanup and maintenance:

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

### Configuration Properties

Add the following properties to your `application.yml`:

```yaml
security:
  logging:
    retention-days: 180  # Number of days to keep logs
    ip-tracking:
      enabled: true
    location-tracking:
      enabled: true
    suspicious-activity:
      enabled: true
      threshold: 3  # Risk level threshold for alerts
```

## API Reference

### ConnectionLogService

Main service for creating and retrieving log entries.

#### Core Methods

```java
// Log a generic user action
ConnectionLog logUserAction(
    User user,
    Device device,
    ActionLogType actionType,
    boolean successful,
    String details,
    String metadata,
    HttpServletRequest request
);

// Log a login attempt
ConnectionLog logLogin(
    User user,
    Device device,
    boolean successful,
    String failureReason,
    HttpServletRequest request
);

// Get user activity history
Page<ConnectionLog> getUserActionHistory(
    User user,
    List<ActionLogType> actionTypes,
    Instant startDate,
    Instant endDate,
    Pageable pageable
);

// Detect suspicious activities
List<ConnectionLog> detectSuspiciousActivity(User user);
```

### ConnectionLogController

REST controller for accessing logs.

#### Endpoints for Regular Users

```
GET /api/security/logs/my-history
GET /api/security/logs/my-recent-logins
GET /api/security/logs/my-actions
GET /api/security/logs/my-stats
GET /api/security/logs/my-security-alerts
```

#### Endpoints for Administrators

```
GET /api/security/logs/user/{userId}
GET /api/security/logs/user/{userId}/actions
GET /api/security/logs/search
GET /api/security/logs/system-stats
```

## Best Practices

### Logging Strategy

1. **Direct Logging**: Use service methods directly for complex or conditional logging
2. **Aspect-Based Logging**: Use aspects for consistent, non-intrusive logging of common activities
3. **Annotation-Based Logging**: Use `@LogAdminAction` for administrative operations

### Performance Optimization

1. Use asynchronous logging for non-critical activities
2. Implement smart batching for high-volume logs
3. Configure appropriate indexes on the logs table

### Security Enhancements

1. Implement log tamper detection
2. Use encryption for sensitive metadata
3. Apply strict access controls to log data

## Example Implementations

### Manual Logging Example

```java
@Service
public class UserProfileService {
    private final ConnectionLogService logService;
    
    @Autowired
    public UserProfileService(ConnectionLogService logService) {
        this.logService = logService;
    }
    
    public void updateProfile(User user, ProfileUpdateForm form, HttpServletRequest request) {
        // Business logic
        boolean updated = userRepository.updateProfile(user.getId(), form);
        
        // Log the activity
        logService.logUserAction(
            user,
            null,  // Device may be null
            ActionLogType.PROFILE_UPDATED,
            updated,
            "User profile updated: " + getChangedFields(form),
            null,  // No metadata
            request
        );
    }
}
```

### Aspect-Based Logging Example

```java
@Aspect
@Component
public class SecurityLoggingAspect {
    private final ConnectionLogService logService;
    
    @Autowired
    public SecurityLoggingAspect(ConnectionLogService logService) {
        this.logService = logService;
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.service.UserService.resetPassword(..))",
        returning = "result"
    )
    public void logPasswordReset(JoinPoint joinPoint, Object result) {
        Object[] args = joinPoint.getArgs();
        User user = (User) args[0];
        HttpServletRequest request = getCurrentRequest();
        
        logService.logPasswordResetComplete(user, null, request);
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
```

### Annotation-Based Logging Example

```java
@Service
public class AdminService {
    
    @LogAdminAction(
        actionType = ActionLogType.ADMIN_USER_UPDATED,
        description = "Admin modified user account settings"
    )
    public User updateUserSettings(Long userId, UserSettingsForm form) {
        // Implementation
    }
    
    @LogAdminAction(
        actionType = ActionLogType.ADMIN_FORCE_LOGOUT,
        description = "Admin forced user logout"
    )
    public void forceUserLogout(Long userId) {
        // Implementation
    }
}
```

## Troubleshooting

### Common Issues

#### Missing Context Information

**Problem**: IP address or location information is missing in logs.

**Solution**: 
- Ensure the `HttpServletRequest` is properly passed to logging methods
- Check if your application is behind a proxy and configure appropriate headers

#### Performance Impact

**Problem**: Logging system causes performance degradation.

**Solution**:
- Consider using asynchronous logging
- Optimize database queries and indexes
- Implement batch processing for high-volume logs

#### Missing Logs

**Problem**: Expected actions are not being logged.

**Solution**:
- Check aspect pointcut expressions
- Verify that service methods are properly invoking the logging service
- Ensure transaction boundaries don't interfere with log persistence

## Performance Considerations

### Database Optimization

1. **Partitioning**: Consider partitioning logs by date for improved performance
2. **Indexing**: Create indexes for common query patterns
3. **Archiving**: Move older logs to archive tables or storage

### Runtime Efficiency

1. **Asynchronous Processing**: Use `@Async` for non-critical logging operations
2. **Batching**: Batch log entries for bulk inserts during high-load scenarios
3. **Caching**: Cache IP geolocation data to reduce external API calls
