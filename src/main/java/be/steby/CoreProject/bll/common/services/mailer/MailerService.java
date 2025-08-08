package be.steby.CoreProject.bll.common.services.mailer;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import org.springframework.scheduling.annotation.Async;


public interface MailerService {
  @Async
  void sendPasswordReset(String token, User user);

  @Async
  void sendPasswordResetRefresh(String newToken, User user);

  void sendAccountConfirmation(String token, User user, String temporaryPassword);

  void sendNewAccountConfirmation(String token, User user);

  void sendWelcome(User user);

    /**
     * Sends a deactivation request confirmation email to the user
     *
     * @param user The user whose account deactivation was requested
     * @param deactivationReason The reason for deactivation
     * @param reasonDetails Additional details about the deactivation (can be null)
     */
    void sendAccountDeactivationRequest(String token, User user, DeactivationReason deactivationReason, String reasonDetails);

    /**
     * Sends a deactivation confirmation email to the user
     *
     * @param user The user whose account was deactivated
     * @param deactivationReason The reason for deactivation
     * @param reasonDetails Additional details about the deactivation (can be null)
     */
    void sendAccountDeactivationConfirmation(User user, DeactivationReason deactivationReason, String reasonDetails);

    /**
     * Sends a reactivation confirmation email to the user
     *
     * @param token The reactivation token
     * @param user The user whose account reactivation was requested
     */
    void sendAccountReactivationRequest(String token, User user);


    /**
     * Sends a reactivation confirmation email to the user
     *
     * @param user The user whose account was successfully reactivated
     */
    void sendAccountReactivationConfirmation(User user);

  void sendPasswordChangeConfirmation(User user);

  void sendChangeEmailRequest(String token, User user);

  void sendChangeEmailVerification(String token, User user, String newEmail);

  void sendChangeEmailCancellation(User user);

  void sendChangeEmailConfirmation(String token, User user, String newEmail, String email);

  void sendSignUpConfirmation(String token, User user);

  void sendNewDeviceAlert(User user, Device device, String token);

  void sendBlacklistedDeviceAlert(User user, Device device, String token);

}
