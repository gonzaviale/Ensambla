package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuenta;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaRequest;
import com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta.ObtenerResumenDeLaCuentaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/negocios/resumen")
@Tag(name = "Negocios")
@RequiredArgsConstructor
public class ResumenDeLaCuentaController {

    private final ObtenerResumenDeLaCuenta obtenerResumenDeLaCuenta;

    @GetMapping
    @Operation(summary = "Resumen conjunto de los negocios de la cuenta: ventas, margen, avisos pendientes y estado de "
            + "cada uno en los últimos 30 días, y los totales")
    public ResponseEntity<ObtenerResumenDeLaCuentaResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok(obtenerResumenDeLaCuenta.execute(
                new ObtenerResumenDeLaCuentaRequest(UsuarioId.de(authentication.getName()))));
    }
}
