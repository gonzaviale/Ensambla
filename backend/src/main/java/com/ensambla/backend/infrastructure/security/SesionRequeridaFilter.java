package com.ensambla.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Las escrituras privadas sin sesión responden 401 antes de validar su CSRF. */
@RequiredArgsConstructor
public class SesionRequeridaFilter extends OncePerRequestFilter {

    private final RequestMatcher rutasPublicas;
    private final SecurityErrorHandler securityErrorHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return rutasPublicas.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            securityErrorHandler.commence(request, response,
                    new InsufficientAuthenticationException("Se requiere una sesión válida"));
            return;
        }
        chain.doFilter(request, response);
    }
}

