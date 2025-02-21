package be.steby.CoreProject.pl.security.models;



import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserRegisterForm {

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
  private String username;

  @NotBlank(message = "Firstname cannot be blank")
  @Size(min = 2, max = 50, message = "Firstname must be between 2 and 50 characters")
  private String firstname;

  @NotBlank(message = "Lastname cannot be blank")
  @Size(min = 2, max = 50, message = "Lastname must be between 2 and 50 characters")
  private String lastname;

  //@AlreadyExist(message = "This email is already in use")
  @Email(message = "Invalid email format")
  @NotBlank(message = "Email cannot be blank")
  private String email;

  @NotBlank(message = "Phone number cannot be blank")
  @Size(min = 9, max = 15, message = "Phone number must be between 9 and 15 characters")
  @Pattern(regexp = "^[0-9]+$", message = "Phone number must contain only digits")
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  private Set<UserRole> userRoles;

  public User toEntity() {
    return new User(username, firstname, lastname, email, phoneNumber, userRoles);
  }
}
