package uk.gov.hmcts.juror.scheduler.config.scheduler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.juror.standard.components.JwtAuthenticationEntryPoint;
import uk.gov.hmcts.juror.standard.service.exceptions.UnauthorisedException;

import java.io.IOException;

/** Converts JWT parsing failures thrown before Spring Security's exception translation filter. */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
public class JwtFailureFilter extends OncePerRequestFilter {

    private final JwtAuthenticationEntryPoint entryPoint;

    public JwtFailureFilter(JwtAuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath();
        String authorization = request.getHeader("Authorization");
        // Shared user-management expressions assume an authenticated principal.
        if (path.startsWith("/auth/") && !("POST".equals(request.getMethod()) && "/auth/login".equals(path))
            && (authorization == null || !authorization.startsWith("Bearer "))) {
            entryPoint.commence(request, response, new BadCredentialsException("Missing credentials"));
            return;
        }
        try {
            chain.doFilter(request, response);
        } catch (UnauthorisedException exception) {
            entryPoint.commence(request, response, new BadCredentialsException("Invalid credentials", exception));
        }
    }
}
