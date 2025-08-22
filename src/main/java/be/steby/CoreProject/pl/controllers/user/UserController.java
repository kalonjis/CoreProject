package be.steby.CoreProject.pl.controllers.user;

import be.steby.CoreProject.bll.domains.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


}
