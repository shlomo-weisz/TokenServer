package com.tokenlearn.server.service;

import com.tokenlearn.server.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class GoogleTokenVerifier {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final JwtDecoder jwtDecoder;
    private final String googleClientId;

    public GoogleTokenVerifier(@Value("${google.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId;
        this.jwtDecoder = JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
    }

    public GoogleUserClaims verify(String idToken) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE_AUTH_NOT_CONFIGURED", "Google auth is not configured");
        }

        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            validateAudience(jwt);
            validateEmail(jwt);
            return new GoogleUserClaims(
                    jwt.getClaimAsString("sub"),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    jwt.getClaimAsString("picture"));
        } catch (BadJwtException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        }
    }

    private void validateAudience(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(googleClientId)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token audience mismatch");
        }
    }

    private void validateEmail(Jwt jwt) {
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google account email is not verified");
        }
    }

    public record GoogleUserClaims(
            String subject,
            String email,
            String givenName,
            String familyName,
            String pictureUrl) {
    }
}
