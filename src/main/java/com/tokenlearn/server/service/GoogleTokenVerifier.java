package com.tokenlearn.server.service;

import com.tokenlearn.server.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GoogleTokenVerifier {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private volatile JwtDecoder jwtDecoder;
    private final Set<String> allowedClientIds;
    private final RestClient restClient;

    public GoogleTokenVerifier(@Value("${google.client-id:}") String googleClientId) {
        this.allowedClientIds = Arrays.stream(googleClientId.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
        this.restClient = RestClient.create();
    }

    public GoogleUserClaims verify(String idToken) {
        if (allowedClientIds.isEmpty()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE_AUTH_NOT_CONFIGURED", "Google auth is not configured");
        }

        try {
            Jwt jwt = decoder().decode(idToken);
            validateAudience(jwt);
            validateIssuer(jwt);
            validateEmail(jwt);
            return new GoogleUserClaims(
                    jwt.getClaimAsString("sub"),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    jwt.getClaimAsString("picture"));
        } catch (AppException ex) {
            throw ex;
        } catch (BadJwtException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        } catch (JwtException ex) {
            return verifyViaTokenInfo(idToken);
        } catch (RuntimeException ex) {
            return verifyViaTokenInfo(idToken);
        }
    }

    private GoogleUserClaims verifyViaTokenInfo(String idToken) {
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri(GOOGLE_TOKENINFO_URL + "?id_token={idToken}", idToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
            }
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_PROVIDER_UNAVAILABLE", "Google token verification is temporarily unavailable");
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_PROVIDER_UNAVAILABLE", "Google token verification is temporarily unavailable");
        }

        if (tokenInfo == null || tokenInfo.isEmpty()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        }

        validateTokenInfoAudience(tokenInfo);
        validateTokenInfoIssuer(tokenInfo);
        validateTokenInfoExpiry(tokenInfo);

        String email = toText(tokenInfo.get("email"));
        boolean emailVerified = toBoolean(tokenInfo.get("email_verified"));
        if (!StringUtils.hasText(email) || !emailVerified) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google account email is not verified");
        }

        String subject = toText(tokenInfo.get("sub"));
        if (!StringUtils.hasText(subject)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        }

        return new GoogleUserClaims(
                subject,
                email,
                toText(tokenInfo.get("given_name")),
                toText(tokenInfo.get("family_name")),
                toText(tokenInfo.get("picture")));
    }

    private JwtDecoder decoder() {
        JwtDecoder local = jwtDecoder;
        if (local == null) {
            synchronized (this) {
                local = jwtDecoder;
                if (local == null) {
                    local = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
                    jwtDecoder = local;
                }
            }
        }
        return local;
    }

    private void validateIssuer(Jwt jwt) {
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (!GOOGLE_ISSUER.equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token issuer mismatch");
        }
    }

    private void validateAudience(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        String authorizedParty = jwt.getClaimAsString("azp");
        boolean audienceAllowed = audience != null && audience.stream().anyMatch(allowedClientIds::contains);
        boolean azpAllowed = StringUtils.hasText(authorizedParty) && allowedClientIds.contains(authorizedParty);
        if (!audienceAllowed && !azpAllowed) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token audience mismatch");
        }
    }

    private void validateEmail(Jwt jwt) {
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        if (emailVerified == null) {
            emailVerified = toBoolean(jwt.getClaims().get("email_verified"));
        }
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google account email is not verified");
        }
    }

    private void validateTokenInfoAudience(Map<String, Object> tokenInfo) {
        String aud = toText(tokenInfo.get("aud"));
        String azp = toText(tokenInfo.get("azp"));
        boolean audienceAllowed = StringUtils.hasText(aud) && allowedClientIds.contains(aud);
        boolean azpAllowed = StringUtils.hasText(azp) && allowedClientIds.contains(azp);
        if (!audienceAllowed && !azpAllowed) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token audience mismatch");
        }
    }

    private void validateTokenInfoIssuer(Map<String, Object> tokenInfo) {
        String issuer = toText(tokenInfo.get("iss"));
        if (!GOOGLE_ISSUER.equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token issuer mismatch");
        }
    }

    private void validateTokenInfoExpiry(Map<String, Object> tokenInfo) {
        String exp = toText(tokenInfo.get("exp"));
        if (!StringUtils.hasText(exp)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token is expired");
        }

        try {
            long expEpoch = Long.parseLong(exp);
            if (expEpoch <= Instant.now().getEpochSecond()) {
                throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google token is expired");
            }
        } catch (NumberFormatException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid Google token");
        }
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    public record GoogleUserClaims(
            String subject,
            String email,
            String givenName,
            String familyName,
            String pictureUrl) {
    }
}
