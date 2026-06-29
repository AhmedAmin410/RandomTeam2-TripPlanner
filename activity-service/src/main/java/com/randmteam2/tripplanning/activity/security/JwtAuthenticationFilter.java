package com.randmteam2.tripplanning.activity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/activities/health".equals(path)
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        AuthContext context = new AuthContext(request);

        AuthHandler extraction = new TokenExtractionHandler();
        AuthHandler signature  = new SignatureValidationHandler(jwtService);
        AuthHandler role       = new RoleAuthorizationHandler();

        extraction.setNext(signature).setNext(role);
        extraction.handle(context);

        if (context.isFailed()) {
            response.sendError(context.getStatusCode(), context.getErrorMessage());
            return;
        }

        if (context.getEmail() != null && context.getRole() != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    context.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + context.getRole()))
            );
            auth.setDetails(context.getUserId());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
