package be.steby.CoreProject.pl.models.admin;



import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;


public record UserRegisterForm (

  @NotBlank(message = "Username cannot be blank")
  @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
  String username,

  @NotBlank(message = "Firstname cannot be blank")
  @Size(min = 2, max = 50, message = "Firstname must be between 2 and 50 characters")
  String firstname,

  @NotBlank(message = "Lastname cannot be blank")
  @Size(min = 2, max = 50, message = "Lastname must be between 2 and 50 characters")
  String lastname,

  //@AlreadyExist(message = "This email is already in use")
  @Email(message = "Invalid email format")
  @NotBlank(message = "Email cannot be blank")
  String email,

  @NotBlank(message = "Phone number cannot be blank")
  @Size(min = 9, max = 15, message = "Phone number must be between 9 and 15 characters")
  @Pattern(regexp = "^[0-9]+$", message = "Phone number must contain only digits")
  String phoneNumber,

  @Enumerated(EnumType.STRING)
  Set<UserRole> userRoles
  ){

  public User toEntity() {
    Set<UserRole> allRoles = new HashSet<>();
    for (UserRole role : userRoles) {
      allRoles.addAll(UserRole.setRoles(role));
    }
    return new User(
            username,
            firstname,
            lastname,
            email,
            phoneNumber,
            allRoles
    );
  }
}
