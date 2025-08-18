package be.steby.CoreProject.bll.common.services.validation;

import be.steby.CoreProject.bll.common.services.validation.textField.TextFieldValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TextFieldValidationServiceImpl implements TextFieldValidationService {

    @Value("${validation.username.min-length:2}")
    private int usernameMinLength;

    @Value("${validation.username.max-length:50}")
    private int usernameMaxLength;

    @Value("${validation.firstname.min-length:2}")
    private int firstnameMinLength;

    @Value("${validation.firstname.max-length:100}")
    private int firstnameMaxLength;

    @Value("${validation.lastname.min-length:2}")
    private int lastnameMinLength;

    @Value("${validation.lastname.max-length:100}")
    private int lastnameMaxLength;

    @Value("${validation.phone.min-length:9}")
    private int phoneMinLength;

    @Value("${validation.phone.max-length:15}")
    private int phoneMaxLength;

    @Value("${validation.phone.pattern:^[0-9]+$}")
    private String phonePattern;

    @Override
    public void validateUsername(String username, List<String> errors) {
        validateText(username, "username", usernameMinLength, usernameMaxLength,
                "^[a-zA-Z0-9_-]+$", errors);

        if (username != null && !username.isBlank()) {
            String trimmed = username.trim();
            if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
                errors.add("Username can only contain letters, numbers, hyphens and underscores");
            }
        }
    }

    @Override
    public void validateFirstname(String firstname, List<String> errors) {
        validateText(firstname, "first name", firstnameMinLength, firstnameMaxLength, null, errors);
    }

    @Override
    public void validateLastname(String lastname, List<String> errors) {
        validateText(lastname, "last name", lastnameMinLength, lastnameMaxLength, null, errors);
    }

    @Override
    public void validatePhoneNumber(String phoneNumber, List<String> errors) {
        validateText(phoneNumber, "phone number", phoneMinLength, phoneMaxLength,
                phonePattern, errors);
    }

    @Override
    public void validateText(String text, String fieldName, int minLength, int maxLength,
                             String pattern, List<String> errors) {
        if (text == null || text.isBlank()) {
            errors.add("The " + fieldName + " cannot be empty");
            return;
        }

        String trimmed = text.trim();

        if (trimmed.length() < minLength) {
            errors.add("The " + fieldName + " must contain at least " + minLength + " characters");
        }

        if (trimmed.length() > maxLength) {
            errors.add("The " + fieldName + " cannot exceed " + maxLength + " characters");
        }

        if (pattern != null && !trimmed.matches(pattern)) {
            errors.add("The " + fieldName + " does not meet the required format");
        }
    }
}