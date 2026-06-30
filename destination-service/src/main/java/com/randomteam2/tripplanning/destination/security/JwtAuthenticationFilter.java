package com.randomteam2.tripplanning.destination.security;

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
        return "/api/destinations/health".equals(path)
                || "/error".equals(path)
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            AuthHandler tokenExtraction = new TokenExtractionHandler();
            AuthHandler signatureValidation = new SignatureValidationHandler(jwtService);
            AuthHandler userLoader = new UserLoaderHandler();
            AuthHandler roleAuthorization = new RoleAuthorizationHandler();

            tokenExtraction
                    .setNext(signatureValidation)
                    .setNext(userLoader)
                    .setNext(roleAuthorization);

            AuthContext context = new AuthContext(request, response);
            tokenExtraction.handle(context);

            if (context.isAuthenticated()) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                context.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + context.getRole()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(exception.getStatus());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"" + exception.getMessage() + "\"}");
        }
    }
}
