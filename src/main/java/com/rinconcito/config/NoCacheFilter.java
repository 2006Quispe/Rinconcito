package com.rinconcito.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NoCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ruta = request.getRequestURI();

        if (!esArchivoEstatico(ruta)) {
            response.setHeader(
                    "Cache-Control",
                    "no-cache, no-store, must-revalidate, max-age=0"
            );
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        filterChain.doFilter(request, response);
    }

    private boolean esArchivoEstatico(String ruta) {
        return ruta.startsWith("/css/")
                || ruta.startsWith("/js/")
                || ruta.startsWith("/images/")
                || ruta.startsWith("/img/")
                || ruta.startsWith("/uploads/")
                || ruta.startsWith("/webjars/")
                || ruta.equals("/favicon.ico");
    }
}