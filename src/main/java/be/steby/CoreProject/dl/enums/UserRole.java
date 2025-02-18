package be.steby.CoreProject.dl.enums;


import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    MODERATOR,
    USER,
    GUEST;

    public static Set<UserRole> setRoles(UserRole topRole) {
        return Arrays.stream(UserRole.values())
                .filter(role -> role.ordinal() >= topRole.ordinal())
                .collect(Collectors.toSet());
    }

}

