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
        boolean get = "GET".equalsIgnoreCase(request.getMethod());
        return "/api/itineraries/health".equals(path)
                || path.startsWith("/actuator")
                // Internal Feign endpoints called by user-service without a gateway JWT
                || path.matches("/api/itineraries/user/[^/]+/summary")
                || path.matches("/api/itineraries/user/[^/]+/active-count")
                || path.matches("/api/itineraries/user/[^/]+/completed-count")
                || (get && path.matches("/api/itineraries/[^/]+"))
                || (get && path.matches("/api/itineraries/destination/[^/]+/active-count"))
                || (get && path.matches("/api/itineraries/destination/[^/]+/booking-revenue"))
                || (get && path.matches("/api/itineraries/destination/[^/]+/dashboard-aggregate"))
                || "/api/itineraries/batch".equals(path);
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
                            List.of(new SimpleGrantedAuthority("ROLE_" + ctx.getRole()))
                    );
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
}
