package be.steby.CoreProject.pl.models.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ChangeEmailForm(
        @NotNull
        @Email
        String email,
        String confirmEmail
) {

}
