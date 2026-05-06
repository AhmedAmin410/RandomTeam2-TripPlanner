package com.randmteam2.tripplanning.user.security;

import com.randmteam2.tripplanning.user.model.User;
import com.randmteam2.tripplanning.user.model.UserStatus;
import com.randmteam2.tripplanning.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private JwtService jwtService;
    @Autowired @Lazy private UserDetailsService userDetailsService;
    @Autowired private UserRepository userRepository;

    private static final Pattern ADMIN_ROLE_PATTERN =
            Pattern.compile("^PUT:/api/users/\\d+/role$");

    private String resolveRequiredRole(HttpServletRequest request) {
        String key = request.getMethod() + ":" + request.getServletPath();
        if (ADMIN_ROLE_PATTERN.matcher(key).matches()) return "ADMIN";
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/users/health".equals(path)
                || "/api/users/register".equals(path)
                || "/api/users/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/login".equals(path)
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requiredRole = resolveRequiredRole(request);
        AuthHandler head = new TokenExtractionHandler();
        head.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler())
                .setNext(new RoleAuthorizationHandler(requiredRole));

        AuthContext ctx = new AuthContext(request);
        try {
            head.handle(ctx);
            String email = ctx.claims.getSubject();
            User userFromDb = userRepository.findByEmail(email).orElse(null);
            if (userFromDb == null || userFromDb.getStatus() == UserStatus.DEACTIVATED) {
                throw new AuthException(401, "User account is deactivated or does not exist");
            }
            String role = ctx.claims.get("role", String.class);
            var auth = new UsernamePasswordAuthenticationToken(
                    ctx.claims.getSubject(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(e.getStatus());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
