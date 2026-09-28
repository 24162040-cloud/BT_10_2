package vn.iotstar.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.entity.User;
import vn.iotstar.models.*;
import vn.iotstar.services.AuthenticationService;
import vn.iotstar.services.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService authentication;
    private final JwtService jwt;

    public AuthenticationController(AuthenticationService authentication, JwtService jwt) {
        this.authentication = authentication;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    public User register(@Valid @RequestBody RegisterUserModel input) { return authentication.signup(input); }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginUserModel input) {
        return new LoginResponse(jwt.generateToken(authentication.authenticate(input)), jwt.getExpirationTime());
    }
}
