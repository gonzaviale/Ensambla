package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.auth.iniciarSesion.IniciarSesion;
import com.ensambla.backend.domain.useCase.auth.obtenerSesion.ObtenerSesion;
import com.ensambla.backend.domain.useCase.auth.registrarUsuario.RegistrarUsuario;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.infrastructure.presentation.dto.auth.LoginRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.auth.UsuarioResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.auth.AuthPresentationMapper;
import com.ensambla.backend.infrastructure.security.SesionAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Acceso")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrarUsuario registrarUsuario;
    private final IniciarSesion iniciarSesion;
    private final ObtenerSesion obtenerSesion;
    private final AuthPresentationMapper authPresentationMapper;
    private final SesionAdapter sesionAdapter;

    @PostMapping("/register")
    @Operation(summary = "Registrar un usuario y su negocio sin abrir sesión")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioResponse response = authPresentationMapper.toResponse(
                registrarUsuario.execute(authPresentationMapper.toRequest(request)));
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Validar credenciales y abrir una sesión por cookie HttpOnly")
    public ResponseEntity<UsuarioResponse> iniciar(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        UsuarioResponse usuario = authPresentationMapper.toResponse(
                iniciarSesion.execute(authPresentationMapper.toRequest(body)));
        sesionAdapter.iniciar(usuario.id(), "PERSONA", request, response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usuario);
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidar la sesión y eliminar su cookie")
    public ResponseEntity<Void> cerrar(HttpServletRequest request, HttpServletResponse response) {
        sesionAdapter.cerrar(request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el usuario activo de la sesión")
    public ResponseEntity<UsuarioResponse> actual(Authentication authentication) {
        UsuarioResponse response = authPresentationMapper.toResponse(
                obtenerSesion.execute(UUID.fromString(authentication.getName())));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtener el token CSRF para las escrituras de la sesión")
    public ResponseEntity<CsrfResponse> csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }
}
