//package be.steby.CoreProject.pl.assemblers;
//
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.enums.UserRole;
//import be.steby.CoreProject.pl.controllers.admin.AdminUserController;
//import be.steby.CoreProject.pl.security.user.models.UserDTO;
//import org.springframework.hateoas.EntityModel;
//import org.springframework.hateoas.server.RepresentationModelAssembler;
//import org.springframework.stereotype.Component;
//
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
//import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
//
//
//@Component
//public class UserModelAssembler implements RepresentationModelAssembler<User, EntityModel<UserDTO>> {
//
//    @Override
//    public EntityModel<UserDTO> toModel(User user) {
//        UserDTO userDTO = UserDTO.fromEntity(user);
//        return EntityModel.of(userDTO,
//                linkTo(methodOn(AdminUserController.class).getUserById(userDTO.id())).withSelfRel());
//    }
//
//    // Méthode étendue qui prend en compte les autorisations
////    public EntityModel<UserDTO> toModelWithPermissions(User user, User authenticatedUser, boolean isSuperAdmin) {
////        UserDTO userDTO = UserDTO.fromEntity(user);
////        EntityModel<UserDTO> model = EntityModel.of(userDTO);
////
////        // Lien de base (détails de l'utilisateur)
////        model.add(linkTo(methodOn(AdminUserController.class).getUserById(userDTO.id())).withSelfRel());
////
////        // N'ajoute les liens d'activation/désactivation que si l'utilisateur n'est pas lui-même
////        if (!userDTO.id().equals(authenticatedUser.getId())) {
////            model.add(linkTo(methodOn(AdminUserController.class).activateUser(userDTO.id())).withRel("activate"));
////            model.add(linkTo(methodOn(AdminUserController.class).deactivateUser(userDTO.id())).withRel("deactivate"));
////
////            // Vérifie si l'utilisateur affiché a le rôle SUPER_ADMIN
////            boolean isUserSuperAdmin = userDTO.userRoles().contains(UserRole.SUPER_ADMIN);
////
////            // Seul un SUPER_ADMIN peut modifier un autre SUPER_ADMIN
////            if (!isUserSuperAdmin || isSuperAdmin) {
////                model.add(linkTo(methodOn(AdminUserController.class).grantUserRole(userDTO.id(), null))
////                        .withRel("grant-role"));
////                model.add(linkTo(methodOn(AdminUserController.class).revokeUserRole(userDTO.id(), null))
////                        .withRel("revoke-role"));
////                model.add(linkTo(methodOn(AdminUserController.class).deleteUser(userDTO.id()))
////                        .withRel("delete"));
////            }
////        }
//
////        return model;
////    }
//}