package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQr;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQr;
import com.ensambla.backend.domain.useCase.caja.consultarCobroQr.ConsultarCobroQr;
import com.ensambla.backend.domain.useCase.caja.consultarCobroQr.ConsultarCobroQrRequest;
import com.ensambla.backend.domain.useCase.caja.recibirNotificacionCobroQr.RecibirNotificacionCobroQr;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/caja/cobros-qr")
@Tag(name = "Cobro con QR", description = "Cobrar mostrando un QR híbrido de Mercado Pago")
@RequiredArgsConstructor
public class CobroQrController {

    private final GenerarCobroQr generarCobroQr;
    private final CancelarCobroQr cancelarCobroQr;
    private final ConsultarCobroQr consultarCobroQr;
    private final RecibirNotificacionCobroQr recibirNotificacionCobroQr;
    private final CobroQrPresentationMapper cobroQrPresentationMapper;

    @PostMapping
    @Operation(summary = "Generar el cobro con QR de un importe una sola vez")
    public ResponseEntity<CobroQrWebResponse> generar(Authentication authentication,
            @Valid @RequestBody GenerarCobroQrWebRequest request) {
        return ResponseEntity.ok(cobroQrPresentationMapper.toResponse(generarCobroQr.execute(
                cobroQrPresentationMapper.toGenerarRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar y conciliar el estado del cobro con Mercado Pago")
    public ResponseEntity<CobroQrWebResponse> consultar(Authentication authentication, @PathVariable CobroQrId id) {
        return ResponseEntity.ok(cobroQrPresentationMapper.toResponse(consultarCobroQr.execute(
                new ConsultarCobroQrRequest(UsuarioId.de(authentication.getName()), id))));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un cobro con QR que todavía no se cobró")
    public ResponseEntity<CobroQrWebResponse> cancelar(Authentication authentication,
            @PathVariable CobroQrId id) {
        return ResponseEntity.ok(cobroQrPresentationMapper.toResponse(cancelarCobroQr.execute(
                cobroQrPresentationMapper.toCancelarRequest(UsuarioId.de(authentication.getName()), id))));
    }

    @PostMapping("/mercadopago")
    @Operation(summary = "Recibir una notificación firmada de una order de Mercado Pago")
    public ResponseEntity<Void> notificar(@RequestParam String type,
            @RequestParam("data.id") String recursoId,
            @RequestHeader(value = "x-signature", required = false) String firma,
            @RequestHeader(value = "x-request-id", required = false) String solicitudId) {
        recibirNotificacionCobroQr.execute(type, recursoId, firma, solicitudId);
        return ResponseEntity.ok().build();
    }
}
