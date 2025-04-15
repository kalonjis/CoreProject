package be.steby.CoreProject.il.audit;

import be.steby.CoreProject.bll.services.ConnectionLogService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ActionLogType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect pour journaliser automatiquement les actions des utilisateurs
 * en fonction des méthodes exécutées dans l'application.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionLogAspect {

    private final ConnectionLogService connectionLogService;
    private final SecurityService securityService;
    private final UserService userService;

    /**
     * Journalise les connexions utilisateur
     */
    /**
     * Journalise les connexions utilisateur réussies
     */
    @AfterReturning(
            pointcut = "execution(* be.steby.CoreProject.pl.security.AuthController.login(..))")
    public void logSuccessfulLogin(JoinPoint joinPoint) {
        try {
            // Une fois que login() a réussi, l'utilisateur est authentifié
            if (!securityService.isAnonymous()) {
                User user = securityService.getAuthenticatedUser();
                HttpServletRequest request = getCurrentRequest();

                // Pas besoin de récupérer l'appareil, on peut passer null
                connectionLogService.logLogin(user, null, true, null, request);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la connexion: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les tentatives de connexion échouées
     */
    @AfterThrowing(
            pointcut = "execution(* be.steby.CoreProject.pl.security.AuthController.login(..))",
            throwing = "ex")
    public void logFailedLogin(JoinPoint joinPoint, Exception ex) {
        try {
            // Essayer de récupérer le nom d'utilisateur depuis les arguments
            Object[] args = joinPoint.getArgs();
            String username = null;

            if (args.length > 0 && args[0] != null) {
                // Supposons que le premier argument est le LoginForm
                try {
                    Method usernameMethod = args[0].getClass().getMethod("username");
                    username = (String) usernameMethod.invoke(args[0]);
                } catch (Exception e) {
                    log.warn("Impossible d'extraire le nom d'utilisateur: {}", e.getMessage());
                }
            }

            // Récupérer l'utilisateur si possible (peut être null)
            User user = null;
            if (username != null) {
                try {
                    user = userService.getUserByUsername(username);
                } catch (Exception e) {
                    // Ignorer, l'utilisateur peut ne pas exister
                }
            }

            HttpServletRequest request = getCurrentRequest();
            String failureReason = ex.getMessage();

            connectionLogService.logLogin(user, null, false, failureReason, request);
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de l'échec de connexion: {}", e.getMessage(), e);
        }
    }


    /**
     * Journalise les déconnexions utilisateur
     */
    @Before("execution(* be.steby.CoreProject.pl.security.AuthController.logout(..))")
    public void logLogout(JoinPoint joinPoint) {
        try {
            if (securityService.isAnonymous()) {
                return;
            }

            User user = securityService.getAuthenticatedUser();
            HttpServletRequest request = getCurrentRequest();

            connectionLogService.logLogout(user, null, request);
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la déconnexion: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les confirmations d'appareil
     */
    @AfterReturning(
            pointcut = "execution(* be.steby.CoreProject.bll.services.DeviceService.confirmDevice(..))",
            returning = "device")
    public void logDeviceConfirmation(JoinPoint joinPoint, Device device) {
        try {
            if (device != null) {
                User user = device.getUser();
                HttpServletRequest request = getCurrentRequest();

                connectionLogService.logDeviceConfirmation(user, device, request);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la confirmation d'appareil: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les rejets d'appareil
     */
    @AfterReturning("execution(* be.steby.CoreProject.bll.services.DeviceService.rejectDevice(..))")
    public void logDeviceRejection(JoinPoint joinPoint) {
        try {
            // Le token est le premier argument
            String token = (String) joinPoint.getArgs()[0];
            log.info("Rejet d'appareil avec token: {}", token);

            // Note: Nous ne pouvons pas accéder directement à l'appareil ici,
            // car il est récupéré à l'intérieur de la méthode rejectDevice.
            // Une alternative serait de modifier le service pour qu'il retourne l'appareil rejeté.

            if (!securityService.isAnonymous()) {
                User user = securityService.getAuthenticatedUser();
                HttpServletRequest request = getCurrentRequest();

                connectionLogService.logUserAction(
                        user, null, ActionLogType.DEVICE_REJECTED,
                        true, "Appareil rejeté par l'utilisateur", null, request
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation du rejet d'appareil: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les changements de mot de passe
     */
    @AfterReturning("execution(* be.steby.CoreProject.bll.services.security.AuthService.changePassword(..))")
    public void logPasswordChange(JoinPoint joinPoint) {
        try {
            if (securityService.isAnonymous()) {
                return;
            }

            User user = securityService.getAuthenticatedUser();
            HttpServletRequest request = getCurrentRequest();

            connectionLogService.logPasswordChange(user, null, true, request);
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation du changement de mot de passe: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les réinitialisations de mot de passe
     */
    @AfterReturning("execution(* be.steby.CoreProject.bll.services.security.AuthService.resetPassword(..))")
    public void logPasswordReset(JoinPoint joinPoint) {
        try {
            // Nous ne pouvons pas accéder à l'utilisateur directement ici,
            // car il est récupéré à l'intérieur de la méthode resetPassword.
            // Une solution serait de modifier le service pour retourner l'utilisateur.

            log.info("Réinitialisation de mot de passe effectuée");
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la réinitialisation de mot de passe: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les confirmations d'inscription
     */
    @AfterReturning(
            pointcut = "execution(* be.steby.CoreProject.bll.services.security.AuthService.confirmNewUserAccount(..))",
            returning = "user")
    public void logAccountConfirmation(JoinPoint joinPoint, User user) {
        try {
            if (user != null) {
                HttpServletRequest request = getCurrentRequest();

                connectionLogService.logAccountActivation(user, null, request);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de l'activation de compte: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les inscriptions utilisateur
     */
    @AfterReturning(
            pointcut = "execution(* be.steby.CoreProject.bll.services.security.AuthService.signup(..))",
            returning = "user")
    public void logSignup(JoinPoint joinPoint, User user) {
        try {
            if (user != null) {
                HttpServletRequest request = getCurrentRequest();

                connectionLogService.logAccountCreation(user, null, request);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la création de compte: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise les demandes de changement d'email
     */
    @AfterReturning("execution(* be.steby.CoreProject.bll.services.security.AuthService.changeEmailRequest(..))")
    public void logEmailChangeRequest(JoinPoint joinPoint) {
        try {
            if (securityService.isAnonymous()) {
                return;
            }

            User user = securityService.getAuthenticatedUser();
            HttpServletRequest request = getCurrentRequest();

            // Extraire le nouvel email des arguments
            Object[] args = joinPoint.getArgs();
            String newEmail = null;
            if (args.length > 0 && args[0] != null) {
                // Supposons que le formulaire de changement d'email a une méthode email()
                try {
                    Method emailMethod = args[0].getClass().getMethod("email");
                    newEmail = (String) emailMethod.invoke(args[0]);
                } catch (Exception e) {
                    log.warn("Impossible d'extraire l'email du formulaire: {}", e.getMessage());
                }
            }

            connectionLogService.logEmailChangeRequest(user, null, newEmail, request);
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de la demande de changement d'email: {}", e.getMessage(), e);
        }
    }

    /**
     * Journalise toutes les actions admin marquées avec @LogAdminAction
     */
    @AfterReturning("@annotation(be.steby.CoreProject.il.audit.LogAdminAction)")
    public void logAdminAction(JoinPoint joinPoint) {
        try {
            if (securityService.isAnonymous()) {
                return;
            }

            User admin = securityService.getAuthenticatedUser();
            HttpServletRequest request = getCurrentRequest();

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            LogAdminAction annotation = method.getAnnotation(LogAdminAction.class);
            ActionLogType actionType = annotation.actionType();
            String description = annotation.description();

            if (description.isEmpty()) {
                description = "Action administrative: " + method.getName() + " " +
                        Arrays.toString(joinPoint.getArgs());
            }

            connectionLogService.logUserAction(
                    admin, null, actionType, true, description, null, request
            );
        } catch (Exception e) {
            log.error("Erreur lors de la journalisation de l'action admin: {}", e.getMessage(), e);
        }
    }

    /**
     * Récupère la requête HTTP courante
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}