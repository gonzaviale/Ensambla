package com.ensambla.backend.infrastructure.security;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.NegocioActivoPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

@Component
public class SesionAdapter implements NegocioActivoPort {

    private static final String NEGOCIO_ACTIVO = SesionAdapter.class.getName() + ".NEGOCIO_ACTIVO";

    @Value("${server.servlet.session.cookie.name:ENSAMBLA_SESSION}")
    private String nombreCookie = "ENSAMBLA_SESSION";

    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();

    public void iniciar(UsuarioId usuarioId, String tipo, HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + tipo)));
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(csrfTokenRepository).onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public void cerrar(HttpServletRequest request, HttpServletResponse response) {
        new CsrfLogoutHandler(csrfTokenRepository).logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
        new SecurityContextLogoutHandler().logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
        new CookieClearingLogoutHandler(nombreCookie).logout(request, response, null);
    }

    @Override
    public Optional<NegocioId> obtener(UsuarioId usuarioId) {
        return requestActual()
                .map(request -> request.getSession(false))
                .map(session -> session.getAttribute(NEGOCIO_ACTIVO))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(this::toNegocioId);
    }

    @Override
    public void activar(UsuarioId usuarioId, NegocioId negocioId) {
        requestActual().ifPresent(request ->
                request.getSession(true).setAttribute(NEGOCIO_ACTIVO, negocioId.toString()));
    }

    private Optional<NegocioId> toNegocioId(String valor) {
        try {
            return Optional.of(NegocioId.de(valor));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<HttpServletRequest> requestActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }
}
