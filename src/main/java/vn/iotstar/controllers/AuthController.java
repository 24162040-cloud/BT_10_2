package vn.iotstar.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping({"/", "/login"})
    public String login() { return "forward:/login.html"; }

    @GetMapping("/user/profile")
    public String profile() { return "forward:/profile.html"; }
}
