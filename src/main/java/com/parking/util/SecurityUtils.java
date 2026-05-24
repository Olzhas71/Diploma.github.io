package com.parking.util;

import com.parking.exception.ForbiddenException;
import com.parking.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static Optional<AppUserDetails> currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || !(a.getPrincipal() instanceof AppUserDetails ud)) {
            return Optional.empty();
        }
        return Optional.of(ud);
    }

    public static AppUserDetails requireUser() {
        return currentUser().orElseThrow(() -> new ForbiddenException("Authentication required"));
    }

    public static Long requireUserId() {
        return requireUser().getId();
    }
}
