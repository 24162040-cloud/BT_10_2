package vn.iotstar.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal User user) { return user; }

    @GetMapping({"", "/"})
    public List<User> allUsers() { return users.findAll(); }
}
