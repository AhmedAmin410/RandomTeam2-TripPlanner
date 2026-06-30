package com.randmteam2.tripplanning.itinerary.security;

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
        return "/api/itineraries/health".equals(path)
                || "/error".equals(path)
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        AuthContext ctx = new AuthContext(request);

        AuthHandler tokenExtractor = new TokenExtractionHandler();
        AuthHandler signatureValidator = new SignatureValidationHandler(jwtService);
        AuthHandler userLoader = new UserLoaderHandler();
        AuthHandler roleAuthorizer = new RoleAuthorizationHandler(null);

        tokenExtractor.setNext(signatureValidator);
        signatureValidator.setNext(userLoader);
        userLoader.setNext(roleAuthorizer);

        try {
            tokenExtractor.handle(ctx);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            ctx.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + normalizeRole(ctx.getRole())))
                    );
            authentication.setDetails(ctx.getUserId());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(e.getStatusCode());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            response.getWriter().flush();
        }
    }

    private static String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = role.trim().toUpperCase();
        if ("USER".equals(normalized) || "CUSTOMER".equals(normalized)) {
            return "TRAVELER";
        }
        return normalized;
    }
}
