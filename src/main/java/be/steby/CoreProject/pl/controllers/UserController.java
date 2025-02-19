package be.steby.CoreProject.pl.controllers;

import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.pl.models.user.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    public ResponseEntity<Page<UserDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        Pageable pageable = getPageable(page, size, sort);
        Page<UserDTO> users = userService.searchUsers(null, pageable).map(UserDTO::fromEntity);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserDTO>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserDTO> users = userService.searchUsers(query, pageable).map(UserDTO::fromEntity);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/searchbycriteria")
    public ResponseEntity<Page<UserDTO>> searchByCriteria(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserDTO> users = userService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable)
                .map(UserDTO::fromEntity);
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id){
        return ResponseEntity.ok(
                UserDTO.fromEntity(userService.getUserById(id))
        );
    }


    private Pageable getPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort sorting = Sort.by(Sort.Order.by(sortParams[0])); // Par défaut, tri ASC
        if (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")) {
            sorting = sorting.descending();
        }
        return PageRequest.of(page, size, sorting);
    }


}
