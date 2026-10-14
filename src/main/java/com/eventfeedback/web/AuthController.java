package com.eventfeedback.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells the UI who is using it. Sign-in and sign-out themselves are handled by Spring Security
 * at {@code POST /api/auth/login} and {@code POST /api/auth/logout} (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record CurrentUser(boolean organiser, String username) {
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        if (authentication == null) {
            return new CurrentUser(false, null);
        }
        boolean organiser = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ORGANISER".equals(a.getAuthority()));
        return new CurrentUser(organiser, authentication.getName());
    }
}
