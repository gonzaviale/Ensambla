package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CajaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caja")
@Tag(name = "Caja", description = "Ventas y cobros del negocio de la sesión")
@RequiredArgsConstructor
public class CajaController {

    private final CobrarVenta cobrarVenta;
    private final CajaPresentationMapper cajaPresentationMapper;

    @PostMapping("/ventas")
    @Operation(summary = "Confirmar una compra una sola vez, con su pago y descuento de existencias")
    public ResponseEntity<CobrarVentaWebResponse> cobrar(Authentication authentication,
            @Valid @RequestBody CobrarVentaWebRequest request) {
        return ResponseEntity.ok(cajaPresentationMapper.toResponse(cobrarVenta.execute(
                cajaPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), request))));
    }
}
