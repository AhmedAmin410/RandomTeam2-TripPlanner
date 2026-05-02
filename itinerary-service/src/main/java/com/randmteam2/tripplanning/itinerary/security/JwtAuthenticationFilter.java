package com.randmteam2.tripplanning.itinerary.security;

import com.randmteam2.tripplanning.itinerary.repository.ItineraryRepository;
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
    private final ItineraryRepository itineraryRepository;

    public JwtAuthenticationFilter(JwtService jwtService, ItineraryRepository itineraryRepository) {
        this.jwtService = jwtService;
        this.itineraryRepository = itineraryRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        AuthContext ctx = new AuthContext(request);

        // Build the chain
        AuthHandler tokenExtractor = new TokenExtractionHandler();
        AuthHandler signatureValidator = new SignatureValidationHandler(jwtService);
        AuthHandler userLoader = new UserLoaderHandler(itineraryRepository);
        AuthHandler roleAuthorizer = new RoleAuthorizationHandler(null);

        tokenExtractor.setNext(signatureValidator);
        signatureValidator.setNext(userLoader);
        userLoader.setNext(roleAuthorizer);

        try {
            tokenExtractor.handle(ctx);

            // All handlers passed — populate Spring Security context
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
        }
    }
}