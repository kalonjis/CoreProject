package be.steby.CoreProject.bll.common.services.validation.phone;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneNumberFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for validating and formatting phone numbers for SMS sending.
 * Handles conversion from local formats to international E.164 format.
 */
@Service
@Slf4j
public class PhoneNumberFormatValidationService {

    // Pattern for Belgian mobile numbers (examples: 0498/56 78 90, 0498567890, 0498.56.78.90)
    private static final Pattern BELGIAN_MOBILE_PATTERN = Pattern.compile("^0[4][0-9]{8}$");
    
    // Pattern for international format
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    /**
     * Validates and formats a phone number for SMS sending.
     * 
     * @param phoneNumber The phone number to validate/format
     * @return The number in E.164 format (+32498567890)
     * @throws InvalidPhoneNumberFormatException if the number is invalid
     */
    public String validateAndFormat(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new InvalidPhoneNumberFormatException("Phone number cannot be null or empty");
        }

        // Clean the number (remove spaces, /, -, .)
        String cleaned = cleanPhoneNumber(phoneNumber);
        log.debug("Cleaned phone number: {} -> {}", phoneNumber, cleaned);

        // Check if already in international format
        if (INTERNATIONAL_PATTERN.matcher(cleaned).matches()) {
            log.debug("Phone number already in international format: {}", cleaned);
            return cleaned;
        }

        // Convert Belgian format to international
        if (BELGIAN_MOBILE_PATTERN.matcher(cleaned).matches()) {
            String international = convertBelgianToInternational(cleaned);
            log.debug("Converted Belgian number: {} -> {}", phoneNumber, international);
            return international;
        }

        // If no recognized format
        throw new InvalidPhoneNumberFormatException(
            String.format("Invalid phone number format: %s. Expected Belgian mobile (04xxxxxxxx) or international (+32xxxxxxxxx)", 
                         phoneNumber)
        );
    }

    /**
     * Checks if a number is valid without formatting it.
     */
    public boolean isValid(String phoneNumber) {
        try {
            validateAndFormat(phoneNumber);
            return true;
        } catch (InvalidPhoneNumberFormatException e) {
            return false;
        }
    }

    /**
     * Cleans a phone number by removing all non-numeric characters 
     * except the initial +.
     */
    private String cleanPhoneNumber(String phoneNumber) {
        // Keep the + if it's at the beginning
        boolean startsWithPlus = phoneNumber.startsWith("+");
        
        // Remove everything except digits
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        // Put back the + if necessary
        return startsWithPlus ? "+" + digitsOnly : digitsOnly;
    }

    /**
     * Converts a Belgian number (0498567890) to international format (+32498567890).
     */
    private String convertBelgianToInternational(String belgianNumber) {
        // Remove initial 0 and add +32
        return "+32" + belgianNumber.substring(1);
    }

    /**
     * Formats a number for display (masked for security).
     */
    public String formatForDisplay(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        
        String cleaned = cleanPhoneNumber(phoneNumber);
        if (cleaned.length() >= 4) {
            return cleaned.substring(0, cleaned.length() - 4) + "XXXX";
        }
        
        return "****";
    }
}