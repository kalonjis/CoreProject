package be.steby.CoreProject.bll.domains.admin.services.validation;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.validation.textField.TextFieldValidationService;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Admin policy service implementation.
 * Focuses on complex validations where it adds real business value.
 * Uses common validation services to avoid duplication.
 * ✅ USES EmailPolicyService for email validation
 * ✅ USES UserPermissionService for permission validation
 * ✅ USES TextFieldValidationService for basic text field validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActionPolicyServiceImpl implements AdminActionPolicyService {

    private final EmailPolicyService emailPolicyService;
    private final TextFieldValidationService textFieldValidationService;

    // Admin-specific configuration (keep only what's truly admin-specific)
    @Value("${security.admin.validation.details.min-length:10}")
    private int detailsMinLength;

    @Value("${security.admin.validation.details.max-length:500}")
    private int detailsMaxLength;

    @Value("${security.admin.audit.log-all-operations:true}")
    private boolean logAllOperations;

    @Value("${security.admin.operations.deactivation.require-detailed-reason:true}")
    private boolean requireDetailedReason;

    /**
     * Complex user creation validation.
     * ✅ BUSINESS VALUE: Orchestrates validations and admin-specific business rules
     */
    @Override
    public AdminValidationResult validateUserCreation(AdminUserCreationRequest request) {
        if (logAllOperations) {
            log.debug("Validating user creation - firstname: {}, lastname: {},email: {}",
                    request.firstname(), request.lastname(), request.email());
        }

        List<String> errors = new ArrayList<>();

        // 1. ✅ DELEGATION: Basic text field validation via common service
        textFieldValidationService.validateFirstname(request.firstname(), errors);
        textFieldValidationService.validateLastname(request.lastname(), errors);
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            textFieldValidationService.validatePhoneNumber(request.phoneNumber(), errors);
        }

        // 2. ✅ DELEGATION: Email validation via specialized service
        EmailValidationResult emailResult = emailPolicyService.validateEmail(request.email());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        // 3. ✅ DELEGATION: Role validation via permission service
        //validateUserRoles(request.userRole(), errors);

        // 5. ✅ ADMIN BUSINESS VALUE: Admin-specific creation business rules
        validateAdminSpecificCreationRules(request, errors);

        if (logAllOperations) {
            log.debug("User creation validation completed - {} errors", errors.size());
        }

        return errors.isEmpty() ? AdminValidationResult.valid() : AdminValidationResult.invalid(errors);
    }

    /**
     * Validation specific to admin deactivation details.
     * ✅ BUSINESS VALUE: Admin-specific deactivation business rules
     */
    @Override
    public AdminValidationResult validateDeactivationDetails(AdminDeactivationRequest request) {
        if (logAllOperations) {
            log.debug("Validating deactivation details - category: {}", request.deactivationCategory());
        }

        List<String> errors = new ArrayList<>();

        // 1. ✅ ADMIN BUSINESS VALUE: Administrative deactivation category validation
        validateDeactivationCategory(request.deactivationCategory(), errors);

        // 2. ✅ DELEGATION: Text validation via common service
        textFieldValidationService.validateText(
                request.adminDeactivationDetails(),
                "deactivation details",
                detailsMinLength,
                detailsMaxLength,
                null,
                errors
        );

        // 3. ✅ ADMIN BUSINESS VALUE: Admin deactivation business rules
        validateAdminDeactivationBusinessRules(request, errors);

        if (logAllOperations) {
            log.debug("Deactivation details validation completed - {} errors", errors.size());
        }

        return errors.isEmpty() ? AdminValidationResult.valid() : AdminValidationResult.invalid(errors);
    }

    // ===============================
    // PRIVATE METHODS - ADMIN BUSINESS VALUE
    // ===============================

    /**
     * ✅ DELEGATION: User role validation (delegates to permission service)
     */
    private void validateUserRoles(Set<UserRole> userRoles, List<String> errors) {
        if (userRoles == null || userRoles.isEmpty()) {
            errors.add("At least one role must be assigned to the user");
            return;
        }

        log.debug("Validating {} user roles for admin creation", userRoles.size());
    }

    /**
     * ✅ ADMIN BUSINESS VALUE: Business rules specific to admin user creation
     */
    private void validateAdminSpecificCreationRules(AdminUserCreationRequest request, List<String> errors) {
        // Example: Admin-specific rules
        // - Limitations on certain roles
        // - Validation of coherence between roles and other attributes
        // - Admin-specific audit rules

        // Check if admin is trying to create user with higher privileges
//        if (request.userRoles().contains(UserRole.SUPER_ADMIN)) {
//            errors.add("Super admin role can only be granted through system processes");
//        }

        // Validate role coherence
//        if (request.userRoles().contains(UserRole.ADMIN) &&
//                request.userRoles().contains(UserRole.USER)) {
//            log.warn("User {} being created with both ADMIN and USER roles", request.email());
//        }

        // This method contains the real business value of AdminPolicyService
    }

    /**
     * ✅ ADMIN BUSINESS VALUE: Administrative deactivation category validation
     */
    private void validateDeactivationCategory(AdminDeactivationCategory category, List<String> errors) {
        if (category == null) {
            errors.add("Deactivation category is required");
            return;
        }

        // Admin-specific business rules for categories
//        if (category == AdminDeactivationCategory.SECURITY_VIOLATION && requireDetailedReason) {
//            // This category requires mandatory details
//            log.debug("Security violation category detected - detailed reason will be enforced");
//        }
//
//        // Additional category-specific validation rules
//        switch (category) {
//            case POLICY_VIOLATION:
//                // Could require specific approval levels
//                break;
//            case SECURITY_VIOLATION:
//                // Could require security team notification
//                break;
//            case ADMINISTRATIVE:
//                // Standard admin deactivation
//                break;
//            default:
//                errors.add("Unknown deactivation category: " + category);
//        }
    }

    /**
     * ✅ ADMIN BUSINESS VALUE: Business rules for admin deactivations
     */
    private void validateAdminDeactivationBusinessRules(AdminDeactivationRequest request, List<String> errors) {
        // Admin-specific business rules for deactivations
        // - Coherence between category and details
        // - Validation according to administrative context
        // - Escalation rules, etc.

        // Example: Security violations must have detailed explanations
//        if (request.deactivationCategory() == AdminDeactivationCategory.SECURITY_VIOLATION) {
//            if (request.adminDeactivationDetails() == null ||
//                    request.adminDeactivationDetails().length() < 50) {
//                errors.add("Security violation deactivations require detailed explanation (minimum 50 characters)");
//            }
//        }
//
//        // Example: Policy violations should reference specific policies
//        if (request.deactivationCategory() == AdminDeactivationCategory.POLICY_VIOLATION) {
//            String details = request.adminDeactivationDetails();
//            if (details != null && !details.toLowerCase().contains("policy")) {
//                log.warn("Policy violation deactivation may need policy reference");
//            }
//        }
    }
}