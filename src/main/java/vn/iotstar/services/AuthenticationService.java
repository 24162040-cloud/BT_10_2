package vn.iotstar.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.iotstar.entity.User;
import vn.iotstar.models.LoginUserModel;
import vn.iotstar.models.RegisterUserModel;
import vn.iotstar.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager manager;

    public AuthenticationService(UserRepository users, PasswordEncoder encoder, AuthenticationManager manager) {
        this.users = users;
        this.encoder = encoder;
        this.manager = manager;
    }

    public User signup(RegisterUserModel input) {
        checkPasswordLength(input.password());
        String email = input.email().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        return users.saveAndFlush(new User(input.fullName().strip(), email, encoder.encode(input.password())));
    }

    public User authenticate(LoginUserModel input) {
        checkPasswordLength(input.password());
        return (User) manager.authenticate(new UsernamePasswordAuthenticationToken(
                input.email().toLowerCase(Locale.ROOT), input.password())).getPrincipal();
    }

    private void checkPasswordLength(String password) {
        // BCrypt's limit is 72 bytes, not 72 Unicode characters.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8");
        }
    }
}
