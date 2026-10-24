package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.pago.actualizarRenovacion.ActualizarRenovacion;
import com.ensambla.backend.domain.useCase.pago.cancelarRenovacion.CancelarRenovacion;
import com.ensambla.backend.domain.useCase.pago.consultarRenovacion.ConsultarRenovacion;
import com.ensambla.backend.domain.useCase.pago.contratarPremium.ContratarPremium;
import com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago.RecibirNotificacionPago;
import com.ensambla.backend.infrastructure.presentation.dto.pago.RenovacionResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.pago.PagoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pagos Premium")
public class PagoController {

    private final ConsultarRenovacion consultarRenovacion;

    private final ContratarPremium contratarPremium;

    private final CancelarRenovacion cancelarRenovacion;

    private final ActualizarRenovacion actualizarRenovacion;

    private final RecibirNotificacionPago recibirNotificacionPago;

    private final PagoPresentationMapper pagoPresentationMapper;

    @GetMapping("/api/planes/renovacion")
    @Operation(summary = "Consultar la renovación del negocio de la sesión")
    public ResponseEntity<RenovacionResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                consultarRenovacion.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion")
    @Operation(summary = "Preparar o recuperar el checkout de renovación automática")
    public ResponseEntity<RenovacionResponse> contratar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                contratarPremium.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion/cancelar")
    @Operation(summary = "Cancelar la renovación conservando el tiempo pagado y de prueba")
    public ResponseEntity<RenovacionResponse> cancelar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                cancelarRenovacion.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion/actualizar")
    @Operation(summary = "Conciliar la renovación con Mercado Pago sin conceder acceso desde la redirección")
    public ResponseEntity<Void> actualizar(Authentication authentication) {
        actualizarRenovacion.execute(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/api/pagos/mercadopago")
    @Operation(summary = "Recibir una notificación firmada de Mercado Pago")
    public ResponseEntity<Void> notificar(@RequestParam String type, @RequestParam("data.id") String recursoId,
            @RequestHeader(value = "x-signature", required = false) String firma,
            @RequestHeader(value = "x-request-id", required = false) String solicitudId) {
        recibirNotificacionPago.execute(type, recursoId, firma, solicitudId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }
}
