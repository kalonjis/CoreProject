package be.steby.CoreProject.bll.common.services.notification.phone;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Base class for all domain-specific phone services.
 * Provides common utilities for SMS sending such as username formatting,
 * message formatting, and time formatting.
 */
@RequiredArgsConstructor
public abstract class BasePhoneService {

    protected final PhoneUtil phoneUtil;

    @Value("${url.front_server}")
    protected String frontUrl;

    /**
     * SMS message templates from configuration.
     */
    @Value("${phone.sms.templates.two-factor-code:Votre code de vérification : {code}}")
    protected String twoFactorCodeTemplate;

    @Value("${phone.sms.templates.account-notification:Notification de sécurité : {message}}")
    protected String accountNotificationTemplate;

    /**
     * Defines the username to display in SMS messages.
     * Uses firstname if available, otherwise falls back to username.
     *
     * @param user The user
     * @return The display name
     */
    protected String defineUsername(User user) {
        return user.getFirstname() != null ? user.getFirstname() : user.getUsername();
    }

    /**
     * Formats an Instant into a human-readable relative time string.
     *
     * @param instant The instant to format
     * @return A formatted string like "2 hours ago"
     */
    protected String formatTime(Instant instant) {
        if (instant == null) {
            return "Unknown time";
        }

        Instant now = Instant.now();
        Duration duration = Duration.between(instant, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds == 1 ? "1 second ago" : seconds + " seconds ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }

        long days = hours / 24;
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }

        if (days < 30) {
            long weeks = days / 7;
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }

        long months = days / 30;
        return months == 1 ? "1 month ago" : months + " months ago";
    }

    /**
     * Formats an Instant to a readable date string with timezone.
     *
     * @param instant The instant to format
     * @return A formatted date string
     */
    protected String formatDate(Instant instant) {
        if (instant == null) {
            return "Unknown date";
        }

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd MMM yyyy HH:mm:ss z")
                .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }

    /**
     * Formats a message using the configured template.
     * Replaces placeholders in the template with actual values.
     *
     * @param template The message template
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The formatted message
     */
    protected String formatMessage(String template, String... placeholders) {
        String result = template;
        
        // Replace placeholders in pairs (key, value)
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = placeholders[i + 1];
            result = result.replace(placeholder, value);
        }
        
        return result;
    }


    protected void sendSms(String message, String phoneNumber) {
        phoneUtil.sendSms(message, phoneNumber);
    }



    /**
     * Checks if the user has a valid phone number for SMS.
     *
     * @param user The user to check
     * @return true if user has a phone number, false otherwise
     */
    protected boolean hasValidPhoneNumber(User user) {
        return user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty();
    }



}