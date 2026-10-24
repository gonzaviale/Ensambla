package com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion;

import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class SincronizarRenovacion {

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final Clock clock;

    public void execute(UUID id) {
        RenovacionPremium renovacion = renovacionPremiumService.obtener(id);
        Optional<ContratoRemoto> remoto = renovacion.getProveedorId() == null
                ? pasarelaSuscripcionesPort.buscar(renovacion.getId())
                : Optional.of(pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId()));
        if (remoto.isEmpty()) {
            return;
        }
        renovacion = renovacionPremiumService.actualizarContrato(id, remoto.get(), OffsetDateTime.now(clock));
        if (renovacion.isCancelacionSolicitada() && !"CANCELADA".equals(renovacion.getEstado())) {
            pasarelaSuscripcionesPort.cancelar(renovacion.getProveedorId());
            ContratoRemoto cancelado = pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId());
            renovacion = renovacionPremiumService.actualizarContrato(id, cancelado, OffsetDateTime.now(clock));
        }
        for (PagoRemoto pago : pasarelaSuscripcionesPort.listarPagos(renovacion.getProveedorId())) {
            renovacionPremiumService.aplicarPago(id, pago, OffsetDateTime.now(clock));
        }
        renovacionPremiumService.recalcular(renovacion.getNegocioId(), OffsetDateTime.now(clock));
    }
}
