package be.steby.CoreProject.bll.domains.notification.services;

import be.steby.CoreProject.bll.domains.notification.events.NotificationCreatedEvent;
import be.steby.CoreProject.bll.domains.notification.exceptions.NotificationNotFoundException;
import be.steby.CoreProject.bll.domains.notification.models.NotificationRequest;
import be.steby.CoreProject.dal.repositories.NotificationRepository;
import be.steby.CoreProject.dl.entities.Notification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationStatus;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link NotificationService}.
 *
 * <p>Orchestrates notification creation, channel selection, preference checking,
 * and publishes events for delivery. This service follows the event-driven
 * pattern used throughout the project.</p>
 *
 * <h4>Architecture:</h4>
 * <pre>
 * NotificationService.send()
 *       │
 *       ▼
 * 1. Build notification entity
 * 2. Resolve delivery channels (from preferences)
 * 3. Persist notification
 * 4. Publish NotificationCreatedEvent
 * 5. Return immediately
 *       │
 *       ▼
 * Listeners handle delivery:
 *   - InAppNotificationListener (SYNC)
 *   - EmailNotificationListener (ASYNC)
 *   - etc.
 * </pre>
 *
 * <h4>Benefits of Event-Driven Approach:</h4>
 * <ul>
 *   <li><b>Decoupling:</b> Service doesn't know about delivery mechanisms</li>
 *   <li><b>Flexibility:</b> Each listener decides sync/async</li>
 *   <li><b>Extensibility:</b> New channel = new listener</li>
 *   <li><b>Consistency:</b> Same pattern as rest of the project</li>
 * </ul>
 *
 * @see NotificationService
 * @see NotificationCreatedEvent
 * @see NotificationPreferenceService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Send Operations
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Notification send(NotificationRequest request) {
        log.info("Creating notification for user: {}, type: {}, scheduledFor: {}",
                request.recipient().getPublicId(),
                request.type(),
                request.scheduledFor());

        // Build notification entity
        Notification notification = buildNotification(request);

        // Determine delivery channels
        Set<NotificationChannel> channels = resolveChannels(request);
        channels.forEach(notification::addChannel);

        // Persist notification
        notification = notificationRepository.save(notification);

        log.info("✅ Notification created: {} for user: {} via channels: {} (scheduledFor: {})",
                notification.getPublicId(),
                request.recipient().getPublicId(),
                channels,
                notification.getScheduledFor());

        // Publish event for delivery
        if (request.isImmediate()) {
            log.debug("Publishing immediate delivery event for notification {}",
                    notification.getPublicId());
            eventPublisher.publishEvent(new NotificationCreatedEvent(notification, channels));
        } else {
            log.info("📅 Notification {} scheduled for delivery at: {} (will be processed by scheduler)",
                    notification.getPublicId(),
                    notification.getScheduledFor());
        }

        return notification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Notification> sendToMany(
            List<User> recipients,
            NotificationType type,
            String title,
            String body) {
        return sendToMany(recipients, type, title, body, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Notification> sendToMany(
            List<User> recipients,
            NotificationType type,
            String title,
            String body,
            String actionUrl) {
        log.debug("Sending notification to {} recipients, type: {}", recipients.size(), type);

        List<Notification> notifications = new ArrayList<>();

        for (User recipient : recipients) {
            try {
                NotificationRequest request = NotificationRequest.builder()
                        .recipient(recipient)
                        .type(type)
                        .title(title)
                        .body(body)
                        .actionUrl(actionUrl)
                        .build();

                notifications.add(send(request));
            } catch (Exception e) {
                log.error("Failed to send notification to user: {} - {}",
                        recipient.getPublicId(), e.getMessage());
            }
        }

        return notifications;
    }

    // =========================================================================
    // Query Operations
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Notification getByPublicId(String publicId) {
        return notificationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotificationNotFoundException(publicId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Notification getByPublicIdAndUser(String publicId, User user) {
        return notificationRepository.findByPublicIdAndRecipient(publicId, user)
                .orElseThrow(() -> new NotificationNotFoundException(publicId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Notification> getActiveNotifications(User user, Pageable pageable) {
        return notificationRepository.findActiveByRecipient(user, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findUnreadByRecipient(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countUnread(User user) {
        return notificationRepository.countUnreadByRecipient(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasUnread(User user) {
        return notificationRepository.hasUnreadNotifications(user);
    }

    // =========================================================================
    // Status Update Operations
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Notification markAsRead(String publicId, User user) {
        Notification notification = getByPublicIdAndUser(publicId, user);

        if (!notification.isRead()) {
            notification.markAsRead(Instant.now());
            notification = notificationRepository.save(notification);
            log.debug("Notification {} marked as read", publicId);
        }

        return notification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int markAsRead(List<String> publicIds, User user) {
        int count = 0;
        for (String publicId : publicIds) {
            try {
                markAsRead(publicId, user);
                count++;
            } catch (NotificationNotFoundException e) {
                log.warn("Notification not found when marking as read: {}", publicId);
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int markAllAsRead(User user) {
        int count = notificationRepository.markAllAsRead(user, Instant.now());
        log.debug("Marked {} notifications as read for user: {}", count, user.getPublicId());
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Notification dismiss(String publicId, User user) {
        Notification notification = getByPublicIdAndUser(publicId, user);

        if (!notification.isDismissed()) {
            notification.markAsDismissed(Instant.now());
            notification = notificationRepository.save(notification);
            log.debug("Notification {} dismissed", publicId);
        }

        return notification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int dismissAll(User user) {
        int count = notificationRepository.dismissAll(user, Instant.now());
        log.debug("Dismissed {} notifications for user: {}", count, user.getPublicId());
        return count;
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(String publicId, User user) {
        Notification notification = getByPublicIdAndUser(publicId, user);
        notificationRepository.delete(notification);
        log.debug("Notification {} deleted", publicId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteBySource(String sourceDomain, String sourceReferenceId) {
        int count = notificationRepository.deleteBySourceDomainAndSourceReferenceId(
                sourceDomain, sourceReferenceId);
        log.debug("Deleted {} notifications for source: {}/{}", count, sourceDomain, sourceReferenceId);
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteMultiple(List<String> publicIds, User user) {
        int count = 0;
        for (String publicId : publicIds) {
            try {
                delete(publicId, user);
                count++;
            } catch (NotificationNotFoundException e) {
                log.warn("Notification not found when deleting: {}", publicId);
            }
        }
        log.debug("Deleted {} notifications for user: {}", count, user.getPublicId());
        return count;
    }


    // =========================================================================
    // Scheduled Notifications
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int processScheduledNotifications() {
        Instant now = Instant.now();
        List<Notification> scheduled = notificationRepository
                .findScheduledReadyForDelivery(now);

        if (scheduled.isEmpty()) {
            log.trace("No scheduled notifications ready for delivery at {}", now);
            return 0;
        }

        log.info("Processing {} scheduled notification(s) ready for delivery", scheduled.size());

        int processed = 0;
        int failed = 0;

        for (Notification notification : scheduled) {
            try {
                log.debug("Processing scheduled notification: {} (scheduledFor: {}, recipient: {})",
                        notification.getPublicId(),
                        notification.getScheduledFor(),
                        notification.getRecipient().getPublicId());

                // Publish event for delivery
                eventPublisher.publishEvent(
                        new NotificationCreatedEvent(notification, notification.getChannels())
                );
                processed++;

                log.info("✅ Scheduled notification {} dispatched for delivery",
                        notification.getPublicId());

            } catch (Exception e) {
                failed++;
                log.error("❌ Failed to process scheduled notification: {} - {}",
                        notification.getPublicId(), e.getMessage(), e);
                notification.markAsFailed();
                notificationRepository.save(notification);
            }
        }

        if (failed > 0) {
            log.warn("Processed {}/{} scheduled notifications ({} failed)",
                    processed, scheduled.size(), failed);
        }

        return processed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean cancelScheduled(String publicId, User user) {
        Notification notification = getByPublicIdAndUser(publicId, user);

        if (notification.isPending() && notification.isScheduled()) {
            notificationRepository.delete(notification);
            log.debug("Scheduled notification {} cancelled", publicId);
            return true;
        }

        return false;
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Builds a notification entity from a request.
     */
    private Notification buildNotification(NotificationRequest request) {
        return Notification.builder()
                .recipient(request.recipient())
                .type(request.type())
                .priority(request.priority())
                .title(request.title())
                .body(request.body())
                .actionUrl(request.actionUrl())
                .data(request.data())
                .icon(request.icon())
                .scheduledFor(request.scheduledFor())
                .expiresAt(request.expiresAt())
                .sourceDomain(request.sourceDomain())
                .sourceReferenceId(request.sourceReferenceId())
                .status(NotificationStatus.PENDING)
                .build();
    }

    /**
     * Resolves which channels to use for delivery.
     *
     * <p>Priority:</p>
     * <ol>
     *   <li>Explicit channels in request (if specified)</li>
     *   <li>User preferences for the notification type</li>
     *   <li>Default: IN_APP only</li>
     * </ol>
     */
    private Set<NotificationChannel> resolveChannels(NotificationRequest request) {
        // Use explicit channels if specified
        if (request.hasExplicitChannels()) {
            return request.channels();
        }

        // Get channels from user preferences
        Set<NotificationChannel> channels = preferenceService.getEnabledChannels(
                request.recipient(),
                request.type()
        );

        // Filter by quiet hours (unless urgent)
        if (!request.priority().bypassesQuietHours()) {
            channels = filterByQuietHours(request.recipient(), channels);
        }

        // Fallback to IN_APP if no channels
        if (channels.isEmpty()) {
            channels = Set.of(NotificationChannel.IN_APP);
        }

        return channels;
    }

    /**
     * Filters channels by quiet hours.
     */
    private Set<NotificationChannel> filterByQuietHours(
            User user,
            Set<NotificationChannel> channels) {
        LocalTime now = LocalTime.now();

        return channels.stream()
                .filter(channel -> !preferenceService.isInQuietHours(user, channel, now))
                .collect(Collectors.toSet());
    }
}