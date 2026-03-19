package com.tokenlearn.server.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns an RFC 9457 problem document when an unauthenticated request is rejected.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {
                  "type":"urn:tokenlearn:problem:unauthorized",
                  "title":"Unauthorized",
                  "status":401,
                  "detail":"Unauthorized",
                  "code":"UNAUTHORIZED"
                }
                """);
    }
}
