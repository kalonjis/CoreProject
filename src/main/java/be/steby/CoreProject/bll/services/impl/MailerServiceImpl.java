package be.steby.CoreProject.bll.services.impl;


import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.impl.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
@Slf4j
public class MailerServiceImpl implements MailerService {
  private final MailerUtil mailerUtil;
  private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;

  @Value("${url.front_server}")
  private String FRONT_URL;


  /**
   * Sends a password reset email to the user using a reset token.
   *
   * @param token the token used for resetting the password.
   */
  @Async
  @Override
  public void sendPasswordResetEmail(String token, User user) {
    String resetUrl = FRONT_URL + "/reset-password?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getUsername());
    context.setVariable("url", resetUrl);
    context.setVariable("token", token);

    mailerUtil.sendMail("Password Reset", "NewPasswordRequest", context, user.getEmail());
  }

    @Async
    @Override
    public void sendPasswordResetRefreshEmail(String newToken, User user) {
      String resetUrl = FRONT_URL + "/reset-password?token=" + newToken;
      Context context = new Context();
      context.setVariable("username", user.getUsername());
      context.setVariable("url", resetUrl);
      context.setVariable("token", newToken);

      mailerUtil.sendMail("Renouvellement de votre demande de réinitialisation", "PasswordResetRefresh", context, user.getEmail());
    }

  @Override
  public void sendAccountConfirmation(User user, String temporaryPassword) {
    AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createAccountConfirmationToken(user);
    String confirmationUrl = FRONT_URL + "/account-confirmation?token=" + accountConfirmationToken.getToken();
    Context context = new Context();
    context.setVariable("username", user.getFirstname()+ " " + user.getLastname());
    context.setVariable("temporaryPassword", temporaryPassword);
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accountConfirmation", context, user.getEmail());
  }

  @Override
  public void sendNewAccountConfirmation(String token) {

  }

  @Override
  public void sendWelcomeEmail(User user) {

  }

  @Override
  public void sendPasswordResetEmail(String token) {

  }


  /**
 * Sends an account confirmation email to the user with a confirmation token.
 *
 * @param user the user to whom the account confirmation email will be sent.
 */
    //@Async
//    @Override
//    public void sendAccountConfirmation(User user) {
//        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createToken(user, AccountConfirmationToken.class, 86400L);
//        String confirmationUrl = FRONT_URL + "/account-confirmation?token=" + accountConfirmationToken.getToken();
//        Context context = new Context();
//        context.setVariable("username", user.getFirstname()+ " " + user.getLastname());
//        context.setVariable("temporaryPassword", user.getTemporaryPassword());
//        context.setVariable("url", confirmationUrl);
//        mailerUtils.sendMail("Account confirmation", "accountConfirmation", context, user.getEmail());
//    }

  /**
   * Sends a new account confirmation email to the user using an existing token.
   *
   * @param token the token associated with the new account confirmation.
   */
//    @Async
//    @Override
//    public void sendNewAccountConfirmation(String token) {
//        User user = accountConfirmationTokenService.getOne(token).getUser();
//        String confirmationUrl = FRONT_URL + "/account-confirmation?token=" + token;
//        Context context = new Context();
//        context.setVariable("username", user.getFirstname()+ " " + user.getLastname());
//        context.setVariable("temporaryPassword", user.getTemporaryPassword());
//        context.setVariable("url", confirmationUrl);
//        mailerUtils.sendMail("Confirm your Account", "newAccountConfirmationRequest", context, user.getEmail());
//    }

  /**
   * Sends a welcome email to the user.
   *
   * @param user the user to whom the welcome email will be sent.
   */
//  @Async
//  @Override
//  public void sendWelcomeEmail(User user) {
//    Context context = new Context();
//    context.setVariable("username", user.getFirstname() + " " + user.getLastname());
//
//    mailerUtil.sendMail("Welcome to RumeXpert", "GreetingComfirmedUser", context, user.getEmail());
//
//  }


}
