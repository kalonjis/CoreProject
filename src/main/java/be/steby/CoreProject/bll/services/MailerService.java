package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.scheduling.annotation.Async;


public interface MailerService {
  @Async
  void sendPasswordReset(String token, User user);

  @Async
  void sendPasswordResetRefresh(String newToken, User user);

  void sendAccountConfirmation(String token, User user, String temporaryPassword);

  void sendNewAccountConfirmation(String token, User user);

  void sendWelcome(User user);

  void sendPasswordChangeConfirmation(User user);

  void sendChangeEmailRequest(String token, User user);

  void sendChangeEmailVerification(String token, User user, String newEmail);

  void sendChangeEmailConfirmation(String token, User user, String newEmail, String email);

  void sendSignUpConfirmation(String token, User user);

  void sendNewDeviceAlert(User user, Device device, String token);

  void sendBlacklistedDeviceAlert(User user, Device device, String token);

}
