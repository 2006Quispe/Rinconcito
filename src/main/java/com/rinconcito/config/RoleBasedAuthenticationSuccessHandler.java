package com.rinconcito.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.io.IOException;
import java.util.Set;

/**
 * Redirige según el rol tras el login y rechaza accesos cruzados
 * (cliente en login de personal / personal en login de clientes).
 */
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Set<String> ROLES_PERSONAL = Set.of(
            "ROLE_ADMINISTRADOR", "ROLE_MESERO", "ROLE_CAJA"
    );

    private final boolean personalLogin;

    public RoleBasedAuthenticationSuccessHandler(boolean personalLogin) {
        this.personalLogin = personalLogin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        if (personalLogin) {
            if (!ROLES_PERSONAL.contains(role)) {
                reject(request, response, "/personal/login?error");
                return;
            }
        } else {
            if (!"ROLE_CLIENTE".equals(role)) {
                reject(request, response, "/login?error");
                return;
            }
        }

        String destino = switch (role) {
            case "ROLE_ADMINISTRADOR" -> "/admin/dashboard";
            case "ROLE_MESERO" -> "/mesero/mesas";
            case "ROLE_CAJA" -> "/caja/dashboard";
            case "ROLE_CLIENTE" -> "/mi-cuenta";
            default -> "/";
        };

        response.sendRedirect(request.getContextPath() + destino);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String redirect)
            throws IOException {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
        response.sendRedirect(request.getContextPath() + redirect);
    }
}
