package be.steby.CoreProject.bll.common.services.reactivation;

import be.steby.CoreProject.bll.common.models.reactivation.ReactivationEligibility;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ SERVICE TRANSVERSAL - Dans common car utilisé par plusieurs domaines
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReactivationPolicyServiceImpl implements ReactivationPolicyService {


    @Override
    public ReactivationEligibility checkEligibility(User user, User actor) {
        if (user.isAdminDeactivated()) {
            return checkAdminDeactivationEligibility(user, actor);
        } else if (user.isSelfDeactivated()) {
            return checkSelfDeactivationEligibility(user, actor);
        }
        return ReactivationEligibility.notEligible("Unknown deactivation state");
    }

    @Override
    public ReactivationPolicy determineReactivationPolicy(User user, User actor) {
        if (user.isAdminDeactivated()) {
            AdminDeactivationCategory category = user.getAdminDeactivationReason();
            return category.getReactivationPolicy();
        } else if (user.isSelfDeactivated()) {
            return ReactivationPolicy.SELF_SERVICE;
        }
        throw new IllegalStateException("Cannot determine reactivation policy for unknown deactivation state");
    }

    @Override
    public boolean canReactivateWithPolicy(ReactivationPolicy policy, User actor, User target) {
        return switch (policy) {
            case NEVER -> false;
            case SELF_SERVICE -> actor.equals(target) || actor.hasAdminPrivileges();
            case ADMIN_ONLY -> actor.hasAdminPrivileges();
            case SUPER_ADMIN_ONLY -> actor.isSuperAdmin();
        };
    }

    // ===== PRIVATE METHODS =====

    private ReactivationEligibility checkAdminDeactivationEligibility(User user, User actor) {
        AdminDeactivationCategory category = user.getAdminDeactivationReason();
        ReactivationPolicy policy = category.getReactivationPolicy();

        log.debug("Checking admin deactivation eligibility - Category: {}, Policy: {}",
                category, policy);

        if (!canReactivateWithPolicy(policy, actor, user)) {
            return switch (policy) {
                case NEVER -> ReactivationEligibility.neverReactivatable("Legal/compliance restriction");
                case SELF_SERVICE -> ReactivationEligibility.insufficientPermissions("Requires self or admin");
                case ADMIN_ONLY -> ReactivationEligibility.insufficientPermissions("Requires ADMIN");
                case SUPER_ADMIN_ONLY -> ReactivationEligibility.insufficientPermissions("Requires SUPER_ADMIN");
            };
        }

        return ReactivationEligibility.eligible();
    }

    private ReactivationEligibility checkSelfDeactivationEligibility(User user, User actor) {
        // Self-deactivation peut toujours être réactivée par l'utilisateur ou un admin
        if (actor == null || actor.equals(user) || actor.hasAdminPrivileges()) {
            return ReactivationEligibility.eligible();
        }
        return ReactivationEligibility.insufficientPermissions("Self-deactivation requires self or admin");
    }
}