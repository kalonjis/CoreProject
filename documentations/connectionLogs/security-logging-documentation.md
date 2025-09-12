# Security & Connection Logging System

A comprehensive security logging framework for tracking user activities, security events, and connection attempts in a Spring Boot application.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Components](#key-components)
- [Log Types](#log-types)
- [Implementation Details](#implementation-details)
- [Usage Guide](#usage-guide)
- [Administrator Features](#administrator-features)
- [Data Retention and Management](#data-retention-and-management)
- [Security Considerations](#security-considerations)
- [Extending the System](#extending-the-system)
- [Sample Code](#sample-code)

## Overview

The security logging system provides comprehensive tracking of user activities, authentication events, and security-related operations within the application. It's designed to:

- Track user logins, logouts, and authentication attempts
- Monitor account-related activities (creation, activation, deactivation)
- Log security-sensitive operations (password changes, email updates)
- Track device management (registration, confirmation, trust level changes)
- Detect and report suspicious activities
- Support security audits and compliance requirements
- Provide administrators with tools for analyzing user behavior

## Architecture

The logging system follows a layered architecture with aspect-oriented components for non-intrusive activity tracking:

```
┌───────────────────┐     ┌─────────────────────┐     ┌───────────────────┐
│  Web Controllers  │────▶│ ConnectionLogAspect │────▶│ ConnectionLogDTO  │
└───────────────────┘     └─────────────────────┘     └───────────────────┘
          │                         │                          ▲
          │                         │                          │
          ▼                         ▼                          │
┌───────────────────┐     ┌─────────────────────┐     ┌───────────────────┐
│  Service Methods  │────▶│ ConnectionLogService│────▶│  ConnectionLog    │
└───────────────────┘     └─────────────────────┘     └───────────────────┘
                                    │                          │
                                    ▼                          │
                          ┌─────────────────────┐             │
                          │ConnectionLogRepository│────────────┘
                          └─────────────────────┘
```

### Flow of Information

1. User actions are captured at the controller or service layer
2. The `ConnectionLogAspect` intercepts relevant method calls using AOP
3. The `ConnectionLogService` processes and enriches log data
4. Logs are stored in the database through the `ConnectionLogRepository`
5. Admin interfaces display logs using `ConnectionLogDTO` for presentation

## Key Components

### Core Entities and Models

- **ConnectionLog**: The main entity that stores all log information
- **ActionLogType**: Enum defining all possible action types with categorization
- **ConnectionLogDTO**: Data transfer object for API responses

### Services and Aspects

- **ConnectionLogService**: Main service for creating and retrieving logs
- **ConnectionLogAspect**: AOP component that intercepts method calls to automate logging
- **ConnectionLogControllerAdvisor**: Exception handler for log-related errors

### Controllers and Repositories

- **ConnectionLogController**: REST endpoints for accessing log data
- **ConnectionLogRepository**: JPA repository with specialized query methods

## Log Types

The system categorizes logs by type using the `ActionLogType` enum. Major categories include:

### Authentication
- User logins and logouts
- Failed login attempts
- Token refreshes
- 2FA operations

### Account Management
- Account creation
- Account activation/deactivation
- Account locking/unlocking

### Password Operations
- Password resets
- Password changes
- Password expiration

### Email Operations
- Email change requests
- Email verification
- Email updates

### Device Management
- Device registration
- Device confirmation
- Trust level changes
- Blacklisting/whitelisting

### Admin Operations
- Admin user management
- System configuration changes

### Security Events
- Suspicious activity detection
- Brute force attempts
- Location changes
- IP blocking

## Implementation Details

### Database Schema

The `connection_logs` table stores all activity logs with the following structure:

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

### Entity Relationships

- Each log entry is associated with a `User`
- Logs may optionally be associated with a `Device`
- Multiple log entries can reference the same user or device

### Security Risk Evaluation

The system includes a risk evaluation mechanism that assigns a risk level (0-3) to each log entry based on:

1. The sensitivity of the action (e.g., password changes are higher risk)
2. Device trust level (unknown devices increase risk)
3. IP address history (new IP addresses increase risk)
4. Location changes (unusual locations increase risk)

This enables prioritization of potential security issues.

### Error Handling

The system uses a hybrid approach to error handling:

- Non-critical errors (e.g., metadata formatting) are handled locally
- Critical errors are propagated to the controller advisor for consistent handling
- Scheduled tasks use robust error handling to prevent application failures

## Usage Guide

### Direct Service Usage

To log events programmatically, inject the `ConnectionLogService` and call the appropriate method:

```java
@Autowired
private ConnectionLogService connectionLogService;

public void updateUserEmail(User user, String newEmail) {
    // Business logic...
    
    // Log the email change
    connectionLogService.logEmailChangeRequest(
        user, 
        currentDevice, 
        newEmail, 
        httpRequest
    );
    
    // More business logic...
}
```

### Aspect-Based Automatic Logging

For common actions, configure the `ConnectionLogAspect` to automatically log activities:

```java
// In ConnectionLogAspect.java

@AfterReturning(
    pointcut = "execution(* com.example.service.UserService.changeEmail(..))",
    returning = "result"
)
public void logEmailChange(JoinPoint joinPoint, Object result) {
    // Extract relevant information and log the events
}
```

### Custom Annotation for Admin Actions

For admin operations, use the `@LogAdminAction` annotation:

```java
@LogAdminAction(
    actionType = ActionLogType.ADMIN_USER_UPDATED,
    description = "Admin updated user profile"
)
public void updateUserAsAdmin(Long userId, UserUpdateForm form) {
    // Implementation...
}
```

## Administrator Features

### Log Search and Filtering

Administrators can search logs with multiple criteria:

- By user ID
- By action type
- By time range
- By IP address
- By success/failure status

### Security Statistics

The system provides predefined statistics:

- Login success/failure rates
- Unique users and IP addresses
- Geographic distribution of logins
- Daily login trends

### Suspicious Activity Detection

The system can detect potentially suspicious activities:

- Multiple logins from different locations in a short time
- Failed login attempts from multiple IP addresses
- Unusual device usage patterns

## Data Retention and Management

### Automatic Cleanup

The system includes a scheduled task for log cleanup:

```java
@Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
public void cleanupOldLogs() {
    // Delete logs older than 180 days
    Instant cutoffDate = Instant.now().minus(180, ChronoUnit.DAYS);
    connectionLogRepository.deleteByTimestampBefore(cutoffDate);
}
```

### Data Export

Admin users can export log data for compliance and audit purposes through the API endpoints.

## Security Considerations

### Privacy Protection

- IP addresses and location data are handled according to privacy policies
- Metadata is carefully filtered to prevent PII (Personally Identifiable Information) leakage

### Data Integrity

- Log entries are immutable once created
- The system records both the action and the result (success/failure)
- Timestamps are standardized using UTC time

### Access Control

- Regular users can only access their own logs
- Admin access to logs requires higher trust level for devices
- Sensitive search operations require elevated permissions

## Extending the System

### Adding New Log Types

To add a new log type:

1. Add the type to the `ActionLogType` enum
2. Create appropriate methods in `ConnectionLogService`
3. Update the `ConnectionLogAspect` if automatic logging is needed

### Custom Analytics

The repository layer can be extended with new query methods for specialized analytics:

```java
// Example of a custom analytics query
@Query("SELECT YEAR(cl.timestamp) as year, MONTH(cl.timestamp) as month, " +
       "COUNT(*) as count FROM ActivityLog cl " +
       "WHERE cl.actionType = :actionType " +
       "GROUP BY YEAR(cl.timestamp), MONTH(cl.timestamp)")
List<Object[]> countActionsByMonth(@Param("actionType") String actionType);
```

## Sample Code

### Creating a New Log Entry

```java
ConnectionLog log = ConnectionLog.builder()
    .user(user)
    .device(device)
    .timestamp(Instant.now())
    .ipAddress(clientIp)
    .location(location)
    .successful(true)
    .actionType(ActionLogType.PASSWORD_CHANGED.name())
    .actionDetails("Password changed successfully")
    .sessionId(sessionId)
    .riskLevel(1)
    .build();

connectionLogRepository.save(log);
```

### Retrieving User Activity History

```java
// Get all login activity for a user
Page<ConnectionLog> loginHistory = connectionLogRepository.findByUserAndActionTypeOrderByTimestampDesc(
    user, 
    ActionLogType.AUTH_LOGIN.name(), 
    pageable
);

// Convert to DTOs for API response
Page<ConnectionLogDTO> dtoPage = loginHistory.map(ConnectionLogDTO::fromEntity);
```

### Implementing Security Alerts

```java
// Check if this is a high-risk action
if (riskLevel >= 3) {
    // Trigger security alert
    notificationService.sendSecurityAlert(
        user.getEmail(),
        "Unusual login activity detected",
        "A login from a new location was detected. If this wasn't you, please secure your account."
    );
    
    // Flag the log entry
    log.setTriggeredAlert(true);
}
```
