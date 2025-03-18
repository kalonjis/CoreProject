package be.steby.CoreProject.pl.controllers;


import be.steby.CoreProject.bll.services.AdminService;
import be.steby.CoreProject.bll.services.security.SecurityService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import be.steby.CoreProject.pl.assemblers.UserModelAssembler;
import be.steby.CoreProject.pl.models.admin.UserRoleForm;
import be.steby.CoreProject.pl.models.admin.UserRegisterForm;
import be.steby.CoreProject.pl.models.user.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final SecurityService securityService;
    private final UserModelAssembler userAssembler;
    private final PagedResourcesAssembler<User> pagedResourcesAssembler;

    @GetMapping("/users/all")
    public ResponseEntity<PagedModel<EntityModel<UserDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        Pageable pageable = getPageable(page, size, sort);
        Page<User> userPage = adminService.searchUsers(null, pageable);

        return ResponseEntity.ok(assemblePagedModel(userPage, pageable));
    }

    @GetMapping("/users/search")
    public ResponseEntity<PagedModel<EntityModel<UserDTO>>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<User> userPage = adminService.searchUsers(query, pageable);

        return ResponseEntity.ok(assemblePagedModel(userPage, pageable));
    }

    @GetMapping("/users/searchbycriteria")
    public ResponseEntity<PagedModel<EntityModel<UserDTO>>> searchByCriteria(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<User> userPage = adminService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable);

        return ResponseEntity.ok(assemblePagedModel(userPage, pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<EntityModel<UserDTO>> getUserById(@PathVariable Long id) {
        User user = adminService.getUserById(id);
        User authenticatedUser = securityService.getAuthenticatedUser();
        boolean isSuperAdmin = securityService.authenticatedHasRole(UserRole.SUPER_ADMIN);

        EntityModel<UserDTO> userModel = userAssembler.toModelWithPermissions(user, authenticatedUser, isSuperAdmin);

        return ResponseEntity.ok(userModel);
    }


    @PostMapping("/users")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody UserRegisterForm form) {
        User user = adminService.createUser(form.toEntity());
        String location = "/api/user/" + user.getId();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur créé avec succès");
        response.put("userId", String.valueOf(user.getId()));

        return ResponseEntity.created(URI.create(location)).body(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/activate/{id}")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/deactivate/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/grant-role/{id}")
    public ResponseEntity<Void> grantUserRole(@PathVariable Long id, @Valid @RequestBody UserRoleForm form) {
        adminService.grantUserRole(id, form.userRole());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/revoke-role/{id}")
    public ResponseEntity<Void> revokeUserRole(@PathVariable Long id, @Valid @RequestBody UserRoleForm form) {
        adminService.revokeUserRole(id, form.userRole());
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/users/force-reset-password/{id}")
    public ResponseEntity<Void> forceResetPassword(@PathVariable Long id) {
        adminService.triggerPasswordReset(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/device/list/user/{id}")
    public ResponseEntity<List<Device>> getUserDevices(@PathVariable Long id) {
        List<Device> devices = adminService.getUserDevices(id);
        return ResponseEntity.ok(devices);
    }


    private Pageable getPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = (sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Sort sorting = Sort.by(direction, sortParams[0]);
        return PageRequest.of(page, size, sorting);
    }

    private PagedModel<EntityModel<UserDTO>> assemblePagedModel(Page<User> userPage, Pageable pageable) {
        User authenticatedUser = securityService.getAuthenticatedUser();
        boolean isSuperAdmin = securityService.authenticatedHasRole(UserRole.SUPER_ADMIN);

        // Utiliser un assembleur personnalisé pour convertir les utilisateurs en EntityModel<UserDTO>
        PagedModel<EntityModel<UserDTO>> pagedModel = pagedResourcesAssembler.toModel(
                userPage,
                user -> userAssembler.toModelWithPermissions(user, authenticatedUser, isSuperAdmin)
        );

        // Ajouter des liens supplémentaires
        pagedModel.add(linkTo(methodOn(AdminController.class).register(null)).withRel("create-user"));
        pagedModel.add(linkTo(methodOn(AdminController.class).searchUsers(null, null)).withRel("search"));
        pagedModel.add(linkTo(methodOn(AdminController.class).searchByCriteria(null, null, null, null, null, null)).withRel("advanced-search"));

        return pagedModel;
    }
}

