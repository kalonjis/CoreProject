package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.User;
import org.springframework.scheduling.annotation.Async;


public interface MailerService {
  @Async
  void sendPasswordResetEmail(String token, User user);

  @Async
  void sendPasswordResetRefreshEmail(String newToken, User user);

  void sendAccountConfirmation(User user, String temporaryPassword);

  void sendNewAccountConfirmation(String token);

  void sendWelcomeEmail(User user);

  void sendPasswordResetEmail(String token);
}
