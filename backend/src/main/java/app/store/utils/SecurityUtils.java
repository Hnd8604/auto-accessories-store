package app.store.utils;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
