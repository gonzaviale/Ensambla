package com.ensambla.backend.infrastructure.security;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioActivoFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;

    private final SesionAdapter sesionAdapter;

    private final SecurityErrorHandler securityErrorHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            try {
                usuarioService.obtenerActivo(UUID.fromString(authentication.getName()));
            } catch (CredencialesInvalidasException exception) {
                sesionAdapter.cerrar(request, response);
                securityErrorHandler.commence(
                        request, response, new InsufficientAuthenticationException("Sesión inválida"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
