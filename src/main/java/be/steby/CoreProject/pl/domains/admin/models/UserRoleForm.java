package be.steby.CoreProject.pl.domains.admin.models;

import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.validation.constraints.NotNull;


@NotNull(message = "User role cannot be null")
public record UserRoleForm(
        UserRole userRole
) {
}
