package be.steby.CoreProject.pl.domains.password.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validator implementation for {@link PasswordMatch} annotation.
 *
 * <p>Validates that two password fields in the same object have matching values.
 * Uses reflection to access the field values for comparison.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String firstFieldName;
    private String secondFieldName;
    private String message;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true; // Let @NotNull handle null validation
        }

        try {
            Object firstValue = getFieldValue(obj, firstFieldName);
            Object secondValue = getFieldValue(obj, secondFieldName);

            boolean valid = isFieldValuesEqual(firstValue, secondValue);

            if (!valid) {
                // Disable default violation and add custom message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(secondFieldName) // Point to the confirmation field
                        .addConstraintViolation();
            }

            return valid;

        } catch (Exception e) {
            // If reflection fails, consider validation as failed
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Unable to validate password match: " + e.getMessage())
                    .addConstraintViolation();
            return false;
        }
    }

    /**
     * Gets the value of a field from an object using reflection.
     */
    private Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * Compares two field values for equality, handling null cases properly.
     */
    private boolean isFieldValuesEqual(Object firstValue, Object secondValue) {
        if (firstValue == null && secondValue == null) {
            return true;
        }

        if (firstValue == null || secondValue == null) {
            return false;
        }

        return firstValue.equals(secondValue);
    }
}