package be.steby.CoreProject.pl.domains.profile.models.requests.phone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for updating or setting a phone number.
 * Used when user wants to add/update their phone number before verification.
 */
public record PhoneNumberRequest(
        /**
         * Phone number in international format.
         * Must start with + followed by country code and national number.
         *
         * Length constraints based on ITU-T E.164 standard:
         * - Minimum: 7 digits (e.g., +1234567 for some Pacific islands)
         * - Maximum: 15 digits (international standard limit)
         *
         * Examples:
         * - +32498567890 (Belgium mobile)
         * - +1234567890 (US number)
         * - +8613912345678 (China mobile)
         * - +33123456789 (France)
         * - +4915123456789 (Germany)
         */
        @NotBlank(message = "Phone number is required")
        @Size(min = 7, max = 16, message = "Phone number must be between 7 and 16 characters including country code")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Phone number must be in international format (+country_code followed by 6-14 digits)"
        )
        String phoneNumber

) { }