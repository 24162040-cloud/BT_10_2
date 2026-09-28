package vn.iotstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.filter.JwtAuthenticationFilter;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.services.JwtService;

@Configuration
public class SecurityConfiguration {
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, UserDetailsService users) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, ex) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType("application/problem+json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Thiếu token hoặc token không hợp lệ/hết hạn\"}");
        };
        return http
                // Only Authorization: Bearer is accepted; no cookie/session authentication.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/login.html", "/user/profile",
                                "/profile.html", "/js/**", "/css/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/problem+json;charset=UTF-8");
                            response.getWriter().write("{\"status\":403,\"detail\":\"Không có quyền truy cập\"}");
                        }))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users, unauthorized), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
