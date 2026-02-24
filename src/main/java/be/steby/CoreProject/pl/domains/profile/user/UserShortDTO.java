//package be.steby.CoreProject.pl.domains.profile.user;
//
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.enums.UserRole;
//
//import java.util.Set;
//
//public record UserShortDTO(
//        String publicId,
//        String username,
//        String email,
//        String phoneNumber,
//        Set<UserRole> userRoles
//) {
//    public static UserShortDTO fromEntity(User u){
//        return new UserShortDTO(
//                u.getPublicId(),
//                u.getUsername(),
//                u.getEmail(),
//                u.getPhoneNumber(),
//                u.getUserRoles()
//        );
//    }
//}
