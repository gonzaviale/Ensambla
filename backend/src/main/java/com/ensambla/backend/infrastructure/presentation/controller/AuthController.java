package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro, login, logout y sesión actual llegan con T-005 y T-006; acá sólo vive el token CSRF,
 * que el frontend necesita para cualquier escritura.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Acceso")
public class AuthController {

    @GetMapping("/csrf")
    @Operation(summary = "Obtener el token CSRF para las escrituras de la sesión")
    public ResponseEntity<CsrfResponse> csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }
}
