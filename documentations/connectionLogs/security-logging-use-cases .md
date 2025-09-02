# Security Logging Use Cases and Examples

This document provides concrete use cases and examples for implementing and leveraging the security logging system in real-world scenarios.

## Table of Contents
- [Authentication and Authorization](#authentication-and-authorization)
- [Account Management](#account-management)
- [Device Tracking](#device-tracking)
- [Security Monitoring](#security-monitoring)
- [Audit and Compliance](#audit-and-compliance)
- [Advanced Analytics](#advanced-analytics)
- [Report Generation](#report-generation)

## Authentication and Authorization

### Use Case: Monitoring Authentication Attempts

**Scenario**: Track successful and failed login attempts to detect brute force attacks or stolen credentials.

**Implementation**:

```java
// In AuthController.java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginForm form, HttpServletRequest request) {
    try {
        User user = authService.login(form.username(), form.password());
        Device device = deviceService.detectCurrentDevice(request);
        
        // Log successful login
        connectionLogService.logLogin(user, device, true, null, request);
        
        // Generate tokens and complete login process
        // ...
        
        return ResponseEntity.ok(responseBody);
    }

## Device Tracking

### Use Case: Device Registration and Confirmation

**Scenario**: Track when users register new devices and manage device trust levels.

**Implementation**:

```java
// In DeviceService.java
@Transactional
public Device detectAndRegisterDevice(HttpServletRequest request, User user, boolean confirmDevice) {
    String userAgentString = request.getHeader("User-Agent");
    String ipAddress = IpUtils.getClientIp(request);
    String fingerprint = DeviceDetectionUtils.generateFingerprint(request, user.getId());
    UserAgent agent = userAgentAnalyzer.parse(userAgentString);

    // Check if device already exists
    Optional<Device> existingDevice = deviceRepository.findByFingerprint(fingerprint);
    
    if (existingDevice.isPresent()) {
        Device device = existingDevice.get();
        
        // Update existing device info
        device.setLastSeen(Instant.now());
        device.setLastIpAddress(ipAddress);
        device.setLocation(IpUtils.getLocationFromIp(ipAddress));
        
        // Log device access
        connectionLogService.logUserAction(
            user, 
            device,
            ActionLogType.DEVICE_REGISTERED,
            true,
            "Device accessed: " + device.getDeviceType() + " " + device.getBrowser(),
            null,
            request
        );
        
        return deviceRepository.save(device);
    } else {
        // Create new device
        Device device = new Device();
        device.setUser(user);
        device.setFingerprint(fingerprint);
        device.setFirstSeen(Instant.now());
        device.setLastSeen(Instant.now());
        device.setLastIpAddress(ipAddress);
        device.setLocation(IpUtils.getLocationFromIp(ipAddress));
        device.setDeviceTrustLevel(DeviceTrustLevel.UNTRUSTED);
        device.setConfirmed(false);
        
        // Fill device details
        DeviceDetectionUtils.populateDeviceInfo(device, agent, request);
        
        Device savedDevice = deviceRepository.save(device);
        
        // Log new device registration
        connectionLogService.logDeviceRegistration(user, savedDevice, request);
        
        // Send notification for confirmation if needed
        if (confirmDevice) {
            DeviceConfirmationToken token = deviceConfirmationTokenService.createToken(user, savedDevice.getId());
            mailerService.sendNewDeviceAlert(user, savedDevice, token.getToken());
        }
        
        return savedDevice;
    }
}

// In DeviceController.java
@PatchMapping("/confirm")
public ResponseEntity<?> confirmDevice(@RequestParam String token) {
    DeviceConfirmationToken deviceToken = deviceConfirmationTokenService.getToken(token);
    deviceConfirmationTokenService.verifyTokenValidity(deviceToken);
    
    Long deviceId = deviceToken.getDeviceId();
    Device device = deviceService.getDeviceById(deviceId);
    User user = device.getUser();
    
    // Update device trust level
    device.setDeviceTrustLevel(DeviceTrustLevel.BASIC);
    device.setConfirmed(true);
    device.setBlacklisted(false);
    deviceRepository.save(device);
    
    // Log the confirmation
    HttpServletRequest request = getCurrentRequest();
    connectionLogService.logDeviceConfirmation(user, device, request);
    
    return ResponseEntity.ok().build();
}
```

### Use Case: Device Trust Level Management

**Scenario**: Track changes to device trust levels for security monitoring.

**Implementation**:

```java
@Service
public class DeviceSecurityService {

    @Transactional
    public void updateTrustLevel(Long deviceId, DeviceTrustLevel newLevel) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + deviceId));
        User user = device.getUser();
        
        // Validate that the authenticated user owns this device
        User authenticatedUser = securityService.getAuthenticatedUser();
        if (!authenticatedUser.getId().equals(user.getId())) {
            throw new AccessDeniedException("Cannot modify device trust level for another user");
        }
        
        // Store old level for logging
        DeviceTrustLevel oldLevel = device.getDeviceTrustLevel();
        
        // Update trust level
        device.setDeviceTrustLevel(newLevel);
        deviceRepository.save(device);
        
        // Log the change
        HttpServletRequest request = getCurrentRequest();
        connectionLogService.logDeviceTrustLevelChange(
            user, 
            device, 
            oldLevel.name(), 
            newLevel.name(), 
            request
        );
        
        // Publish events for other components
        eventPublisher.publishEvent(new DeviceTrustLevelChangedEvent(deviceId, newLevel));
    }
}
```

## Security Monitoring

### Use Case: Suspicious Activity Detection

**Scenario**: Detect and alert on suspicious login patterns or access attempts.

**Implementation**:

```java
@Service
public class SecurityMonitoringService {

    @Scheduled(fixedRate = 60 * 60 * 1000) // Run hourly
    public void detectSuspiciousActivity() {
        Instant startWindow = Instant.now().minus(24, ChronoUnit.HOURS);
        List<User> activeUsers = userRepository.findUsersWithRecentActivity(startWindow);
        
        for (User user : activeUsers) {
            // Get user's login history
            List<ConnectionLog> loginLogs = connectionLogRepository
                .findByUserAndActionTypeAndTimestampAfter(
                    user, 
                    ActionLogType.AUTH_LOGIN.name(), 
                    startWindow
                );
            
            // Skip users with no login activity
            if (loginLogs.size() < 2) {
                continue;
            }
            
            // Check for multiple locations in a short time
            Set<String> locations = loginLogs.stream()
                .map(ConnectionLog::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // Check for rapid location changes
            if (locations.size() > 1 && loginLogs.size() >= 3) {
                boolean suspicious = isLocationChangeSuspicious(loginLogs);
                
                if (suspicious) {
                    // Record the suspicious activity
                    HttpServletRequest request = null; // No request context in scheduled task
                    
                    String details = "Multiple logins from different locations detected: " +
                        String.join(", ", locations);
                        
                    connectionLogService.logSuspiciousActivity(
                        user, 
                        null, 
                        details, 
                        3, // High risk level 
                        request
                    );
                    
                    // Send alert to user
                    notificationService.sendSecurityAlert(
                        user.getEmail(),
                        "Suspicious login activity detected",
                        details
                    );
                }
            }
        }
    }
    
    private boolean isLocationChangeSuspicious(List<ConnectionLog> logs) {
        // Sort logs by timestamp
        logs.sort(Comparator.comparing(ConnectionLog::getTimestamp));
        
        // Check for impossible travel time between locations
        for (int i = 0; i < logs.size() - 1; i++) {
            ConnectionLog current = logs.get(i);
            ConnectionLog next = logs.get(i + 1);
            
            String loc1 = current.getLocation();
            String loc2 = next.getLocation();
            
            if (loc1 != null && loc2 != null && !loc1.equals(loc2)) {
                // Calculate time difference in hours
                double hoursDiff = (double) Duration.between(
                    current.getTimestamp(), 
                    next.getTimestamp()
                ).toMinutes() / 60.0;
                
                // Check if travel between locations is physically impossible
                // This is a simplistic check; a real implementation would use
                // geographic coordinates and calculate actual distances
                if (hoursDiff < 3.0) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
```

### Use Case: Failed Login Monitoring

**Scenario**: Monitor and respond to multiple failed login attempts.

**Implementation**:

```java
@Service
public class LoginSecurityService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    @Transactional
    public void checkFailedLoginAttempts(User user, HttpServletRequest request) {
        // Get recent failed login attempts for this user
        Instant checkTime = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        
        long failedAttempts = connectionLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
            user,
            ActionLogType.AUTH_LOGIN_FAILED.name(),
            false,
            checkTime
        );
        
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Lock the account
            user.setLocked(true);
            user.setLockExpiryTime(Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);
            
            // Log the account lock
            connectionLogService.logUserAction(
                user,
                null,
                ActionLogType.ACCOUNT_LOCKED,
                true,
                "Account locked due to multiple failed login attempts",
                null,
                request
            );
            
            // Check if this is potentially a brute force attack
            detectPotentialBruteForce(user, request);
            
            throw new AccountLockedException("Account locked due to multiple failed attempts");
        }
    }
    
    private void detectPotentialBruteForce(User user, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIp(request);
        
        // Check if this IP has attempted to access multiple accounts
        Instant checkTime = Instant.now().minus(1, ChronoUnit.HOURS);
        
        List<Object[]> ipTargets = connectionLogRepository.countDistinctUsersByIpAddressAndActionType(
            ipAddress,
            ActionLogType.AUTH_LOGIN_FAILED.name(),
            checkTime
        );
        
        if (ipTargets.size() > 3) { // IP tried more than 3 different accounts
            // Log potential brute force attack
            connectionLogService.logUserAction(
                user,
                null,
                ActionLogType.SECURITY_BRUTE_FORCE_ATTEMPT,
                true,
                "Potential brute force attack from IP: " + ipAddress +
                " targeting " + ipTargets.size() + " different accounts",
                null,
                request
            );
            
            // Block the IP (implementation depends on your infrastructure)
            ipBlockingService.blockIp(ipAddress, 24); // Block for 24 hours
        }
    }
}
```

## Audit and Compliance

### Use Case: Admin Action Auditing

**Scenario**: Track all administrative actions for accountability and compliance.

**Implementation**:

```java
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    @LogAdminAction(
        actionType = ActionLogType.ADMIN_USER_UPDATED,
        description = "Admin updated user account"
    )
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateForm form) {
        User user = userService.getUserById(id);
        
        // Update user fields
        if (form.getFirstname() != null) {
            user.setFirstname(form.getFirstname());
        }
        
        if (form.getLastname() != null) {
            user.setLastname(form.getLastname());
        }
        
        if (form.getEmail() != null) {
            user.setEmail(form.getEmail());
        }
        
        // Save changes
        userService.saveUser(user);
        
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }
    
    @LogAdminAction(
        actionType = ActionLogType.ADMIN_FORCE_LOGOUT,
        description = "Admin forced user logout"
    )
    @PostMapping("/users/{id}/force-logout")
    public ResponseEntity<?> forceLogout(@PathVariable Long id) {
        User user = userService.getUserById(id);
        
        // Revoke all user tokens
        tokenService.revokeAllUserTokens(user);
        
        return ResponseEntity.ok().build();
    }
}

// This is the aspect that processes the @LogAdminAction annotations
@Aspect
@Component
public class AdminActionLoggingAspect {

    private final ConnectionLogService logService;
    private final SecurityService securityService;
    
    @Autowired
    public AdminActionLoggingAspect(ConnectionLogService logService, SecurityService securityService) {
        this.logService = logService;
        this.securityService = securityService;
    }
    
    @AfterReturning("@annotation(logAdminAction)")
    public void logAdminAction(JoinPoint joinPoint, LogAdminAction logAdminAction) {
        try {
            // Get the admin user
            User admin = securityService.getAuthenticatedUser();
            
            // Get request context
            HttpServletRequest request = getCurrentRequest();
            
            // Extract target user if available (first argument is often user ID)
            String targetInfo = "";
            if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Long) {
                targetInfo = "Target user ID: " + joinPoint.getArgs()[0];
            }
            
            // Create metadata with admin ID and target info
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("adminId", admin.getId());
            metadata.put("adminUsername", admin.getUsername());
            
            if (!targetInfo.isEmpty()) {
                metadata.put("targetInfo", targetInfo);
            }
            
            String metadataJson = null;
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                // Ignore serialization errors
            }
            
            // Log the admin action
            logService.logUserAction(
                admin,
                null,
                logAdminAction.actionType(),
                true,
                logAdminAction.description(),
                metadataJson,
                request
            );
            
        } catch (Exception e) {
            // Log but don't disrupt the flow
            log.error("Failed to log admin action", e);
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
```

### Use Case: Regulatory Compliance Reporting

**Scenario**: Generate compliance reports for regulatory requirements (GDPR, HIPAA, etc.).

**Implementation**:

```java
@Service
public class ComplianceReportingService {
    
    @Transactional(readOnly = true)
    public ComplianceReport generateGdprReport(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        ComplianceReport report = new ComplianceReport();
        report.setReportType("GDPR Compliance");
        report.setPeriodStart(startDate);
        report.setPeriodEnd(endDate);
        report.setGeneratedAt(Instant.now());
        
        // User data access statistics
        Map<String, Object> accessStats = new HashMap<>();
        
        // Count of personal data access events
        long personalDataAccessCount = connectionLogRepository.countByActionTypeInAndTimestampBetween(
            Arrays.asList(
                ActionLogType.PROFILE_UPDATED.name(),
                ActionLogType.EMAIL_CHANGE_COMPLETE.name()
            ),
            startInstant,
            endInstant
        );
        accessStats.put("personalDataAccessCount", personalDataAccessCount);
        
        // Count of data deletion events
        long dataDeletionCount = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.ACCOUNT_DELETED.name(),
            startInstant,
            endInstant
        );
        accessStats.put("dataDeletionCount", dataDeletionCount);
        
        // Count of consent changes
        long consentChangesCount = connectionLogRepository.countByActionTypeAndDetailsContainingAndTimestampBetween(
            ActionLogType.PROFILE_UPDATED.name(),
            "consent",
            startInstant,
            endInstant
        );
        accessStats.put("consentChangesCount", consentChangesCount);
        
        // Record admin access to user data
        List<ConnectionLog> adminAccessLogs = connectionLogRepository.findByActionTypeStartingWithAndTimestampBetween(
            "ADMIN_",
            startInstant,
            endInstant,
            PageRequest.of(0, 10000)
        );
        
        List<Map<String, Object>> adminAccesses = adminAccessLogs.stream()
            .map(log -> {
                Map<String, Object> access = new HashMap<>();
                access.put("adminId", log.getCreatedBy());
                access.put("timestamp", log.getTimestamp());
                access.put("action", log.getActionType());
                access.put("details", log.getActionDetails());
                return access;
            })
            .collect(Collectors.toList());
        
        accessStats.put("adminDataAccesses", adminAccesses);
        report.setStatistics(accessStats);
        
        return report;
    }
    
    @Transactional(readOnly = true)
    public byte[] generateGdprReportPdf(LocalDate startDate, LocalDate endDate) {
        ComplianceReport report = generateGdprReport(startDate, endDate);
        
        // Generate PDF (implementation will depend on PDF library used)
        return pdfGenerationService.generatePdf(report);
    }
}
```

## Advanced Analytics

### Use Case: Security Dashboard

**Scenario**: Provide real-time security analytics for administrators.

**Implementation**:

```java
@RestController
@RequestMapping("/api/admin/security-dashboard")
@PreAuthorize("hasAuthority('ADMIN')")
public class SecurityDashboardController {

    private final ConnectionLogService connectionLogService;
    
    @Autowired
    public SecurityDashboardController(ConnectionLogService connectionLogService) {
        this.connectionLogService = connectionLogService;
    }
    
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Define time ranges
        Instant now = Instant.now();
        Instant last24Hours = now.minus(24, ChronoUnit.HOURS);
        Instant last7Days = now.minus(7, ChronoUnit.DAYS);
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);
        
        // Get login statistics
        dashboard.put("logins24Hours", connectionLogService.getLoginCount(last24Hours, now));
        dashboard.put("logins7Days", connectionLogService.getLoginCount(last7Days, now));
        dashboard.put("logins30Days", connectionLogService.getLoginCount(last30Days, now));
        
        // Get failed login statistics
        dashboard.put("failedLogins24Hours", connectionLogService.getFailedLoginCount(last24Hours, now));
        dashboard.put("failedLogins7Days", connectionLogService.getFailedLoginCount(last7Days, now));
        dashboard.put("failedLogins30Days", connectionLogService.getFailedLoginCount(last30Days, now));
        
        // Get suspicious activity statistics
        dashboard.put("suspiciousActivities24Hours", connectionLogService.getSuspiciousActivityCount(last24Hours, now));
        dashboard.put("suspiciousActivities7Days", connectionLogService.getSuspiciousActivityCount(last7Days, now));
        dashboard.put("suspiciousActivities30Days", connectionLogService.getSuspiciousActivityCount(last30Days, now));
        
        // Get top login locations
        dashboard.put("topLocations", connectionLogService.getTopLoginLocations(last7Days, now, 5));
        
        // Get login trend data (daily logins for the past 30 days)
        dashboard.put("loginTrend", connectionLogService.getLoginsByDay(last30Days, now));
        
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/users-at-risk")
    public ResponseEntity<List<Map<String, Object>>> getUsersAtRisk() {
        List<Map<String, Object>> usersAtRisk = connectionLogService.identifyUsersAtRisk();
        return ResponseEntity.ok(usersAtRisk);
    }
    
    @GetMapping("/recent-suspicious-activity")
    public ResponseEntity<Page<ConnectionLogDTO>> getRecentSuspiciousActivity(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ConnectionLog> logs = connectionLogService.getRecentSuspiciousActivity(pageable);
        Page<ConnectionLogDTO> dtoPage = logs.map(ConnectionLogDTO::fromEntity);
        
        return ResponseEntity.ok(dtoPage);
    }
}

@Service
public class SecurityAnalyticsService {

    private final ConnectionLogRepository connectionLogRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public SecurityAnalyticsService(
            ConnectionLogRepository connectionLogRepository,
            UserRepository userRepository) {
        this.connectionLogRepository = connectionLogRepository;
        this.userRepository = userRepository;
    }
    
    public List<Map<String, Object>> identifyUsersAtRisk() {
        Instant checkPeriod = Instant.now().minus(30, ChronoUnit.DAYS);
        
        // Find users with recent suspicious activity
        List<User> suspiciousUsers = connectionLogRepository.findUsersWithSuspiciousActivity(checkPeriod);
        
        return suspiciousUsers.stream().map(user -> {
            Map<String, Object> userRisk = new HashMap<>();
            userRisk.put("userId", user.getId());
            userRisk.put("username", user.getUsername());
            userRisk.put("email", user.getEmail());
            
            // Count suspicious events by type
            Map<String, Long> riskFactors = new HashMap<>();
            
            long failedLogins = connectionLogRepository.countByUserAndActionTypeAndSuccessfulAndTimestampAfter(
                user, ActionLogType.AUTH_LOGIN_FAILED.name(), false, checkPeriod
            );
            riskFactors.put("failedLogins", failedLogins);
            
            long locationChanges = connectionLogRepository.countByUserAndActionTypeAndTimestampAfter(
                user, ActionLogType.SECURITY_LOCATION_CHANGE.name(), checkPeriod
            );
            riskFactors.put("locationChanges", locationChanges);
            
            long passwordChanges = connectionLogRepository.countByUserAndActionTypeAndTimestampAfter(
                user, ActionLogType.PASSWORD_CHANGED.name(), checkPeriod
            );
            riskFactors.put("passwordChanges", passwordChanges);
            
            long suspiciousEvents = connectionLogRepository.countByUserAndActionTypeAndTimestampAfter(
                user, ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY.name(), checkPeriod
            );
            riskFactors.put("suspiciousEvents", suspiciousEvents);
            
            userRisk.put("riskFactors", riskFactors);
            
            // Calculate overall risk score (simple weighted sum)
            double riskScore = failedLogins * 0.5 +
                              locationChanges * 2.0 +
                              passwordChanges * 0.3 +
                              suspiciousEvents * 5.0;
            
            userRisk.put("riskScore", riskScore);
            
            return userRisk;
        }).sorted(Comparator.comparing(m -> ((Double) m.get("riskScore"))).reversed())
          .collect(Collectors.toList());
    }
}
```

## Report Generation

### Use Case: Generating Activity Reports

**Scenario**: Allow users to view and export their account activity history.

**Implementation**:

```java
@RestController
@RequestMapping("/api/security/reports")
public class SecurityReportController {

    private final ConnectionLogService connectionLogService;
    private final ReportExportService reportExportService;
    
    @Autowired
    public SecurityReportController(
            ConnectionLogService connectionLogService,
            ReportExportService reportExportService) {
        this.connectionLogService = connectionLogService;
        this.reportExportService = reportExportService;
    }
    
    @GetMapping("/my-activity")
    public ResponseEntity<Page<ConnectionLogDTO>> getMyActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<String> types,
            @PageableDefault(size = 20) Pageable pageable) {
        
        User currentUser = securityService.getAuthenticatedUser();
        
        Instant startDate = from != null 
            ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now().minus(30, ChronoUnit.DAYS);
            
        Instant endDate = to != null
            ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now();
            
        List<ActionLogType> actionTypes = null;
        if (types != null && !types.isEmpty()) {
            actionTypes = types.stream()
                .map(type -> {
                    try {
                        return ActionLogType.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        
        Page<ConnectionLog> logs = connectionLogService.getUserActionHistory(
            currentUser, actionTypes, startDate, endDate, pageable);
            
        return ResponseEntity.ok(logs.map(ConnectionLogDTO::fromEntity));
    }
    
    @GetMapping("/my-activity/export")
    public ResponseEntity<byte[]> exportMyActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<String> types,
            @RequestParam(defaultValue = "csv") String format) {
        
        User currentUser = securityService.getAuthenticatedUser();
        
        Instant startDate = from != null 
            ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now().minus(30, ChronoUnit.DAYS);
            
        Instant endDate = to != null
            ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now();
            
        List<ActionLogType> actionTypes = null;
        if (types != null && !types.isEmpty()) {
            actionTypes = types.stream()
                .map(type -> {
                    try {
                        return ActionLogType.valueOf(type);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        
        List<ConnectionLog> logs = connectionLogService.getUserActionHistoryForExport(
            currentUser, actionTypes, startDate, endDate);
            
        byte[] reportContent;
        String filename;
        
        if ("pdf".equalsIgnoreCase(format)) {
            reportContent = reportExportService.generatePdfReport(
                logs, 
                currentUser, 
                startDate, 
                endDate
            );
            filename = "activity_report.pdf";
        } else {
            // Default to CSV
            reportContent = reportExportService.generateCsvReport(
                logs, 
                currentUser, 
                startDate, 
                endDate
            );
            filename = "activity_report.csv";
        }
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType("pdf".equalsIgnoreCase(format) ? 
                MediaType.APPLICATION_PDF : 
                MediaType.parseMediaType("text/csv"))
            .body(reportContent);
    }
    
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/system-report")
    public ResponseEntity<byte[]> generateSystemReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        Instant startDate = from != null 
            ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now().minus(30, ChronoUnit.DAYS);
            
        Instant endDate = to != null
            ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            : Instant.now();
            
        byte[] reportContent = reportExportService.generateSystemSecurityReport(startDate, endDate);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"system_security_report.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(reportContent);
    }
}

@Service
public class ReportExportService {

    private final ConnectionLogRepository connectionLogRepository;
    
    @Autowired
    public ReportExportService(ConnectionLogRepository connectionLogRepository) {
        this.connectionLogRepository = connectionLogRepository;
    }
    
    public byte[] generateCsvReport(
            List<ConnectionLog> logs, 
            User user, 
            Instant startDate, 
            Instant endDate) {
        
        StringBuilder csv = new StringBuilder();
        
        // Add CSV header
        csv.append("Timestamp,Action Type,Description,IP Address,Location,Status\n");
        
        // Add data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
            
        for (ConnectionLog log : logs) {
            csv.append(formatter.format(log.getTimestamp())).append(",");
            csv.append("\"").append(log.getActionType()).append("\",");
            
            // Escape any quotes in the details
            String details = log.getActionDetails() != null 
                ? log.getActionDetails().replace("\"", "\"\"") 
                : "";
            csv.append("\"").append(details).append("\",");
            
            csv.append(log.getIpAddress() != null ? log.getIpAddress() : "").append(",");
            csv.append("\"").append(log.getLocation() != null ? log.getLocation() : "").append("\",");
            csv.append(log.isSuccessful() ? "Success" : "Failed").append("\n");
        }
        
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    public byte[] generatePdfReport(
            List<ConnectionLog> logs, 
            User user, 
            Instant startDate, 
            Instant endDate) {
        
        // PDF generation would typically use a library like iText, Apache PDFBox, etc.
        // For demonstration purposes, here's a simplified implementation
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Security Activity Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add report metadata
            document.add(new Paragraph("\nUser: " + user.getUsername()));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.systemDefault());
            document.add(new Paragraph("Period: " + 
                formatter.format(startDate) + " to " + formatter.format(endDate)));
            document.add(new Paragraph("Generated: " + 
                formatter.format(Instant.now())));
            
            document.add(new Paragraph("\n"));
            
            // Create table for log entries
            PdfPTable table = new PdfPTable(5); // 5 columns
            table.setWidthPercentage(100);
            
            // Define table headers
            Stream.of("Timestamp", "Action", "Details", "Location", "Status")
                .forEach(header -> {
                    PdfPCell cell = new PdfPCell();
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setPadding(5);
                    cell.setPhrase(new Phrase(header));
                    table.addCell(cell);
                });
            
            // Add data rows
            DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
                
            for (ConnectionLog log : logs) {
                table.addCell(timestampFormatter.format(log.getTimestamp()));
                table.addCell(formatActionType(log.getActionType()));
                table.addCell(log.getActionDetails() != null ? log.getActionDetails() : "");
                table.addCell(log.getLocation() != null ? log.getLocation() : "Unknown");
                
                PdfPCell statusCell = new PdfPCell();
                if (log.isSuccessful()) {
                    statusCell.setPhrase(new Phrase("Success"));
                    statusCell.setBackgroundColor(new BaseColor(230, 255, 230)); // Light green
                } else {
                    statusCell.setPhrase(new Phrase("Failed"));
                    statusCell.setBackgroundColor(new BaseColor(255, 230, 230)); // Light red
                }
                table.addCell(statusCell);
            }
            
            document.add(table);
            
            // Add summary section
            document.add(new Paragraph("\nSummary", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            
            // Group logs by action type
            Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(ConnectionLog::getActionType, Collectors.counting()));
                
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.addCell("Action Type");
            summaryTable.addCell("Count");
            
            actionCounts.forEach((action, count) -> {
                summaryTable.addCell(formatActionType(action));
                summaryTable.addCell(count.toString());
            });
            
            document.add(summaryTable);
            
            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
    
    private String formatActionType(String actionType) {
        // Convert enum name to human-readable format
        // e.g., AUTH_LOGIN -> "Authentication: Login"
        try {
            ActionLogType type = ActionLogType.valueOf(actionType);
            return type.getCategory() + ": " + type.getDescription();
        } catch (IllegalArgumentException e) {
            // Fallback if actionType is not a valid enum value
            return actionType.replace('_', ' ');
        }
    }
    
    public byte[] generateSystemSecurityReport(Instant startDate, Instant endDate) {
        // Similar to the user report generation, but with system-wide statistics
        // Implementation would depend on your PDF generation library
        // This would include system-wide statistics, suspicious activity summary, etc.
        
        // Placeholder implementation
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate()); // Landscape format
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("System Security Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add system statistics
            generateSystemStatisticsSection(document, startDate, endDate);
            
            // Add suspicious activity section
            generateSuspiciousActivitySection(document, startDate, endDate);
            
            // Add login distribution section
            generateLoginDistributionSection(document, startDate, endDate);
            
            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate system security report", e);
        }
    }
    
    private void generateSystemStatisticsSection(Document document, Instant startDate, Instant endDate) 
            throws DocumentException {
        
        document.add(new Paragraph("\nSystem Statistics", 
            new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            
        // Get various statistics
        long totalLogins = connectionLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
            ActionLogType.AUTH_LOGIN.name(), true, startDate, endDate);
            
        long failedLogins = connectionLogRepository.countByActionTypeAndSuccessfulAndTimestampBetween(
            ActionLogType.AUTH_LOGIN.name(), false, startDate, endDate);
            
        long suspiciousActivities = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY.name(), startDate, endDate);
            
        long accountLocks = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.ACCOUNT_LOCKED.name(), startDate, endDate);
            
        long passwordChanges = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.PASSWORD_CHANGED.name(), startDate, endDate);
            
        long passwordResets = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.PASSWORD_RESET_COMPLETE.name(), startDate, endDate);
            
        long newAccounts = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.ACCOUNT_CREATED.name(), startDate, endDate);
            
        long deviceRegistrations = connectionLogRepository.countByActionTypeAndTimestampBetween(
            ActionLogType.DEVICE_REGISTERED.name(), startDate, endDate);
        
        // Create a table for statistics
        PdfPTable statsTable = new PdfPTable(4); // 4 columns
        statsTable.setWidthPercentage(100);
        
        // Add statistics to table
        addStatCell(statsTable, "Total Logins", totalLogins);
        addStatCell(statsTable, "Failed Logins", failedLogins);
        addStatCell(statsTable, "Suspicious Activities", suspiciousActivities);
        addStatCell(statsTable, "Account Locks", accountLocks);
        addStatCell(statsTable, "Password Changes", passwordChanges);
        addStatCell(statsTable, "Password Resets", passwordResets);
        addStatCell(statsTable, "New Accounts", newAccounts);
        addStatCell(statsTable, "Device Registrations", deviceRegistrations);
        
        document.add(statsTable);
    }
    
    private void addStatCell(PdfPTable table, String label, long value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label));
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(String.valueOf(value)));
        table.addCell(valueCell);
    }
    
    private void generateSuspiciousActivitySection(Document document, Instant startDate, Instant endDate) 
            throws DocumentException {
        
        document.add(new Paragraph("\n\nSuspicious Activity Summary", 
            new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
            
        // Get top 10 suspicious activities
        List<ConnectionLog> suspiciousLogs = connectionLogRepository.findByActionTypeAndTimestampBetweenOrderByTimestampDesc(
            ActionLogType.SECURITY_SUSPICIOUS_ACTIVITY.name(), startDate, endDate, PageRequest.of(0, 10));
            
        if (suspiciousLogs.isEmpty()) {
            document.add(new Paragraph("No suspicious activities detected in the selected period."));
            return;
        }
        
        PdfPTable suspiciousTable = new PdfPTable(4);
        suspiciousTable.setWidthPercentage(100);
        
        // Add table headers
        Stream.of("Timestamp", "User", "Details", "Risk Level")
            .forEach(header -> {
                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                cell.setPhrase(new Phrase(header));
                suspiciousTable.addCell(cell);
            });
        
        // Add data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
            
        for (ConnectionLog log : suspiciousLogs) {
            suspiciousTable.addCell(formatter.format(log.getTimestamp()));
            suspiciousTable.addCell(log.getUser().getUsername());
            suspiciousTable.addCell(log.getActionDetails() != null ? log.getActionDetails() : "");
            
            PdfPCell riskCell = new PdfPCell();
            if (log.getRiskLevel() != null) {
                riskCell.setPhrase(new Phrase(formatRiskLevel(log.getRiskLevel())));
                
                // Color-code risk level
                if (log.getRiskLevel() >= 3) {
                    riskCell.setBackgroundColor(new BaseColor(255, 200, 200)); // High risk
                } else if (log.getRiskLevel() == 2) {
                    riskCell.setBackgroundColor(new BaseColor(255, 235, 156)); // Medium risk
                } else {
                    riskCell.setBackgroundColor(new BaseColor(255, 255, 200)); // Low risk
                }
            } else {
                riskCell.setPhrase(new Phrase("Unknown"));
            }
            
            suspiciousTable.addCell(riskCell);
        }
        
        document.add(suspiciousTable);
    }
    
    private String formatRiskLevel(int level) {
        switch (level) {
            case 0: return "No Risk";
            case 1: return "Low Risk";
            case 2: return "Medium Risk";
            case 3: return "High Risk";
            default: return "Unknown";
        }
    }
    
    private void generateLoginDistributionSection(Document document, Instant startDate, Instant endDate) 
            throws DocumentException {
        
        document.add(new Paragraph("\n\nLogin Distribution", 
            new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD)));
        
        // Get geographical distribution
        Map<String, Long> locationStats = connectionLogService.getLoginsByLocation(startDate, endDate);
        
        // Get daily login distribution
        Map<LocalDate, Long> dailyStats = connectionLogService.getLoginsByDay(startDate, endDate);
        
        if (locationStats.isEmpty() && dailyStats.isEmpty()) {
            document.add(new Paragraph("No login data available for the selected period."));
            return;
        }
        
        // Create location distribution table
        if (!locationStats.isEmpty()) {
            document.add(new Paragraph("\nGeographical Distribution", 
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                
            PdfPTable locTable = new PdfPTable(2);
            locTable.setWidthPercentage(60);
            
            PdfPCell headerCell1 = new PdfPCell(new Phrase("Location"));
            headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            locTable.addCell(headerCell1);
            
            PdfPCell headerCell2 = new PdfPCell(new Phrase("Login Count"));
            headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            locTable.addCell(headerCell2);
            
            // Sort locations by login count (descending)
            locationStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    locTable.addCell(entry.getKey());
                    locTable.addCell(entry.getValue().toString());
                });
                
            document.add(locTable);
        }
        
        // Create daily login chart (would typically use a charting library)
        if (!dailyStats.isEmpty()) {
            document.add(new Paragraph("\nDaily Login Activity", 
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                
            // As a simple alternative to a chart, we'll use a table
            PdfPTable dayTable = new PdfPTable(2);
            dayTable.setWidthPercentage(60);
            
            PdfPCell headerCell1 = new PdfPCell(new Phrase("Date"));
            headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dayTable.addCell(headerCell1);
            
            PdfPCell headerCell2 = new PdfPCell(new Phrase("Login Count"));
            headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dayTable.addCell(headerCell2);
            
            // Sort dates chronologically
            dailyStats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    dayTable.addCell(entry.getKey().toString());
                    dayTable.addCell(entry.getValue().toString());
                });
                
            document.add(dayTable);
        }
    }
}
```

### Use Case: Integrating with External Monitoring Systems

**Scenario**: Integrate security logs with external SIEM (Security Information and Event Management) systems.

**Implementation**:

```java
@Service
public class SiemIntegrationService {

    private final ConnectionLogRepository connectionLogRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${security.siem.enabled:false}")
    private boolean siemEnabled;
    
    @Value("${security.siem.endpoint:}")
    private String siemEndpoint;
    
    @Value("${security.siem.api-key:}")
    private String siemApiKey;
    
    @Autowired
    public SiemIntegrationService(
            ConnectionLogRepository connectionLogRepository,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.connectionLogRepository = connectionLogRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }
    
    @Scheduled(fixedRate = 5 * 60 * 1000) // Run every 5 minutes
    @Transactional(readOnly = true)
    public void exportLogsToSiem() {
        if (!siemEnabled || siemEndpoint.isEmpty()) {
            return;
        }
        
        try {
            // Get last sync timestamp
            Instant lastSync = getLastSyncTimestamp();
            Instant now = Instant.now();
            
            // Get logs since last sync
            List<ConnectionLog> newLogs = connectionLogRepository.findByTimestampBetweenOrderByTimestamp(
                lastSync, now, PageRequest.of(0, 1000));
                
            if (newLogs.isEmpty()) {
                saveLastSyncTimestamp(now);
                return;
            }
            
            // Convert logs to SIEM format
            List<Map<String, Object>> siemEvents = newLogs.stream()
                .map(this::convertLogToSiemEvent)
                .collect(Collectors.toList());
                
            // Send logs to SIEM
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", siemApiKey);
            
            HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(siemEvents), 
                headers
            );
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                siemEndpoint, 
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // Update last sync timestamp
                saveLastSyncTimestamp(now);
                
                log.info("Successfully exported {} security logs to SIEM", newLogs.size());
            } else {
                log.error("Failed to export logs to SIEM: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error exporting logs to SIEM", e);
        }
    }
    
    private Map<String, Object> convertLogToSiemEvent(ConnectionLog log) {
        Map<String, Object> event = new HashMap<>();
        
        // Basic events properties
        event.put("timestamp", log.getTimestamp().toString());
        event.put("eventType", "security");
        event.put("actionType", log.getActionType());
        event.put("userId", log.getUser().getId());
        event.put("username", log.getUser().getUsername());
        event.put("successful", log.isSuccessful());
        
        // Device information if available
        if (log.getDevice() != null) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", log.getDevice().getId());
            deviceInfo.put("deviceType", log.getDevice().getDeviceType());
            deviceInfo.put("browser", log.getDevice().getBrowser());
            deviceInfo.put("operatingSystem", log.getDevice().getOperatingSystem());
            deviceInfo.put("trustLevel", log.getDevice().getDeviceTrustLevel().name());
            
            event.put("device", deviceInfo);
        }
        
        // Location information
        Map<String, Object> locationInfo = new HashMap<>();
        locationInfo.put("ipAddress", log.getIpAddress());
        locationInfo.put("location", log.getLocation());
        
        event.put("location", locationInfo);
        
        // Action details
        event.put("details", log.getActionDetails());
        
        // Risk information
        if (log.getRiskLevel() != null) {
            event.put("riskLevel", log.getRiskLevel());
            event.put("riskCategory", getRiskCategory(log.getRiskLevel()));
        }
        
        // Application ID for multi-app environments
        event.put("applicationId", "myFavApp");
        
        return event;
    }
    
    private String getRiskCategory(int riskLevel) {
        if (riskLevel >= 3) return "HIGH";
        if (riskLevel == 2) return "MEDIUM";
        if (riskLevel == 1) return "LOW";
        return "NONE";
    }
    
    private Instant getLastSyncTimestamp() {
        // Retrieve from persistent storage (database, file, etc.)
        // For simplicity, this example uses a hypothetical preference service
        String lastSyncStr = preferenceService.getPreference("siem.lastSyncTimestamp");
        
        if (lastSyncStr == null || lastSyncStr.isEmpty()) {
            // Default to 24 hours ago if no previous sync
            return Instant.now().minus(24, ChronoUnit.HOURS);
        }
        
        try {
            return Instant.parse(lastSyncStr);
        } catch (Exception e) {
            log.error("Invalid last sync timestamp format", e);
            return Instant.now().minus(24, ChronoUnit.HOURS);
        }
    }
    
    private void saveLastSyncTimestamp(Instant timestamp) {
        // Save to persistent storage
        preferenceService.setPreference("siem.lastSyncTimestamp", timestamp.toString());
    }
} catch (InvalidCredentialsException e) {
        // Find the user by username to log the failed attempt
        User user = userService.findByUsername(form.username()).orElse(null);
        
        if (user != null) {
            // Log failed login
            connectionLogService.logLogin(
                user, null, false, 
                "Invalid credentials", request
            );
        }
        
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
```

### Use Case: Session Tracking

**Scenario**: Monitor user session duration and activity to detect anomalies.

**Implementation**:

```java
// In AuthController.java
@PostMapping("/logout")
public ResponseEntity<?> logout(HttpServletRequest request) {
    User user = securityService.getAuthenticatedUser();
    Device device = deviceService.detectCurrentDevice(request);
    
    // Calculate session duration
    HttpSession session = request.getSession(false);
    Long sessionDuration = null;
    
    if (session != null) {
        long creationTime = session.getCreationTime();
        long now = System.currentTimeMillis();
        sessionDuration = (now - creationTime) / 1000; // Convert to seconds
    }
    
    // Log the logout
    ConnectionLog logEntry = connectionLogService.logLogout(user, device, request);
    
    // Update the log with session duration if available
    if (sessionDuration != null) {
        logEntry.setDurationSeconds(sessionDuration);
        connectionLogRepository.save(logEntry);
    }
    
    // Complete logout process
    // ...
    
    return ResponseEntity.ok().build();
}
```

## Account Management

### Use Case: New Account Monitoring

**Scenario**: Track new account creation and activation to detect suspicious patterns.

**Implementation**:

```java
// In UserRegistrationService.java
@Transactional
public User registerNewUser(RegistrationForm form, HttpServletRequest request) {
    // Create user account
    User user = new User();
    user.setUsername(form.getUsername());
    user.setEmail(form.getEmail());
    user.setPassword(passwordEncoder.encode(form.getPassword()));
    user.setEnabled(false);
    user.setEmailVerified(false);
    
    userRepository.save(user);
    
    // Log account creation
    Device device = deviceService.detectCurrentDevice(request);
    connectionLogService.logAccountCreation(user, device, request);
    
    // Create and send verification token
    AccountConfirmationToken token = accountConfirmationTokenService.createToken(user);
    mailerService.sendVerificationEmail(user, token.getToken());
    
    return user;
}

// In AccountConfirmationController.java
@GetMapping("/confirm")
public ResponseEntity<?> confirmAccount(@RequestParam String token, HttpServletRequest request) {
    AccountConfirmationToken confirmationToken = tokenService.getToken(token);
    tokenService.verifyTokenValidity(confirmationToken);
    
    User user = confirmationToken.getUser();
    
    // Activate the account
    user.setEnabled(true);
    user.setEmailVerified(true);
    user.setEverActivated(true);
    userRepository.save(user);
    
    // Log the account activation
    Device device = deviceService.detectCurrentDevice(request);
    connectionLogService.logAccountActivation(user, device, request);
    
    return ResponseEntity.ok("Account activated successfully");
}
```

### Use Case: Account Changes Audit

**Scenario**: Track all changes to user account settings for security and customer support.

**Implementation**:

```java
@Service
public class UserProfileService {

    @LogAdminAction(
        actionType = ActionLogType.PROFILE_UPDATED,
        description = "User profile information updated"
    )
    public void updateProfile(UserProfileForm form) {
        User user = securityService.getAuthenticatedUser();
        
        // Track what fields changed
        Map<String, Object> changes = new HashMap<>();
        if (!user.getFirstname().equals(form.getFirstname())) {
            changes.put("firstname", Map.of("from", user.getFirstname(), "to", form.getFirstname()));
            user.setFirstname(form.getFirstname());
        }
        
        if (!user.getLastname().equals(form.getLastname())) {
            changes.put("lastname", Map.of("from", user.getLastname(), "to", form.getLastname()));
            user.setLastname(form.getLastname());
        }
        
        if (!user.getPhoneNumber().equals(form.getPhoneNumber())) {
            changes.put("phoneNumber", Map.of("from", user.getPhoneNumber(), "to", form.getPhoneNumber()));
            user.setPhoneNumber(form.getPhoneNumber());
        }
        
        // Convert changes to JSON for metadata
        String metadata = null;
        try {
            metadata = objectMapper.writeValueAsString(changes);
        } catch (Exception e) {
            log.warn("Failed to serialize profile changes", e);
        }
        
        // Save user changes
        userRepository.save(user);
        
        // Log the action with detailed changes
        HttpServletRequest request = getCurrentRequest();
        connectionLogService.logUserAction(
            user,
            null,
            ActionLogType.PROFILE_UPDATED,
            true,
            "User profile updated: " + String.join(", ", changes.keySet()),
            metadata,
            request
        );
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}