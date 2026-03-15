package com.tokenlearn.server.util;

import com.tokenlearn.server.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/**
 * Helper methods for extracting the authenticated user identity from Spring Security objects.
 */
public final class AuthUtil {
    private AuthUtil() {
    }

    public static Integer requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated");
        }
        return Integer.parseInt(authentication.getPrincipal().toString());
    }
}
