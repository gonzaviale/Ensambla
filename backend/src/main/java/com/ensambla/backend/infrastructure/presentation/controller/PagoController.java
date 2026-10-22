package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.pago.ActualizarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.CancelarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionUseCase;
import com.ensambla.backend.domain.useCase.pago.ContratarPremiumUseCase;
import com.ensambla.backend.domain.useCase.pago.RecibirNotificacionPagoUseCase;
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

    private final ConsultarRenovacionUseCase consultarRenovacionUseCase;

    private final ContratarPremiumUseCase contratarPremiumUseCase;

    private final CancelarRenovacionUseCase cancelarRenovacionUseCase;

    private final ActualizarRenovacionUseCase actualizarRenovacionUseCase;

    private final RecibirNotificacionPagoUseCase recibirNotificacionPagoUseCase;

    private final PagoPresentationMapper pagoPresentationMapper;

    @GetMapping("/api/planes/renovacion")
    @Operation(summary = "Consultar la renovación del negocio de la sesión")
    public ResponseEntity<RenovacionResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                consultarRenovacionUseCase.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion")
    @Operation(summary = "Preparar o recuperar el checkout de renovación automática")
    public ResponseEntity<RenovacionResponse> contratar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                contratarPremiumUseCase.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion/cancelar")
    @Operation(summary = "Cancelar la renovación conservando el tiempo pagado y de prueba")
    public ResponseEntity<RenovacionResponse> cancelar(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pagoPresentationMapper.toResponse(
                cancelarRenovacionUseCase.execute(UUID.fromString(authentication.getName()))));
    }

    @PostMapping("/api/planes/renovacion/actualizar")
    @Operation(summary = "Conciliar la renovación con Mercado Pago sin conceder acceso desde la redirección")
    public ResponseEntity<Void> actualizar(Authentication authentication) {
        actualizarRenovacionUseCase.execute(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/api/pagos/mercadopago")
    @Operation(summary = "Recibir una notificación firmada de Mercado Pago")
    public ResponseEntity<Void> notificar(@RequestParam String type, @RequestParam("data.id") String recursoId,
            @RequestHeader(value = "x-signature", required = false) String firma,
            @RequestHeader(value = "x-request-id", required = false) String solicitudId) {
        recibirNotificacionPagoUseCase.execute(type, recursoId, firma, solicitudId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).build();
    }
}
