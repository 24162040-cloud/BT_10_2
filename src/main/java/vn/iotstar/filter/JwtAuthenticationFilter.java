package vn.iotstar.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import vn.iotstar.services.JwtService;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService users;
    private final AuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(JwtService jwt, UserDetailsService users, AuthenticationEntryPoint entryPoint) {
        this.jwt = jwt;
        this.users = users;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                var user = users.loadUserByUsername(jwt.extractUsername(header.substring(7)));
                new AccountStatusUserDetailsChecker().check(user);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                SecurityContextHolder.setContext(context);
            } catch (AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, ex);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
