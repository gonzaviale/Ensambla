package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQr;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQr;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobroQrWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.GenerarCobroQrWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.caja.CobroQrPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caja/cobros-qr")
@Tag(name = "Cobro con QR", description = "Cobrar una venta mostrando un QR; el comerciante confirma a mano")
@RequiredArgsConstructor
public class CobroQrController {

    private final GenerarCobroQr generarCobroQr;
    private final CancelarCobroQr cancelarCobroQr;
    private final CobroQrPresentationMapper cobroQrPresentationMapper;

    @PostMapping
    @Operation(summary = "Generar el cobro con QR de un importe una sola vez")
    public ResponseEntity<CobroQrWebResponse> generar(Authentication authentication,
            @Valid @RequestBody GenerarCobroQrWebRequest request) {
        return ResponseEntity.ok(cobroQrPresentationMapper.toResponse(generarCobroQr.execute(
                cobroQrPresentationMapper.toGenerarRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un cobro con QR que todavía no se cobró")
    public ResponseEntity<CobroQrWebResponse> cancelar(Authentication authentication,
            @PathVariable CobroQrId id) {
        return ResponseEntity.ok(cobroQrPresentationMapper.toResponse(cancelarCobroQr.execute(
                cobroQrPresentationMapper.toCancelarRequest(UsuarioId.de(authentication.getName()), id))));
    }
}
