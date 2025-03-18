package be.steby.CoreProject.pl.security.models;



import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.security.validators.PasswordMatch;
import be.steby.CoreProject.pl.security.validators.StrongPassword;
import jakarta.validation.constraints.*;


@PasswordMatch
public record UserSignupForm (

        @NotBlank(message = "Username cannot be blank")
        @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
        String username,

        @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide")
        @NotNull(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 55, message = "Le nouveau mot de passe doit contenir entre 8 et 55 caractères")
        @StrongPassword
        String password,

        @NotBlank(message = "La confirmation du mot de passe ne peut pas être vide")
        @NotNull(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword,

        //@AlreadyExist(message = "This email is already in use")
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be blank")
        String email
  ){

  public User toEntity() {
    return new User(
            username,
            email,
            password
    );
  }
}
