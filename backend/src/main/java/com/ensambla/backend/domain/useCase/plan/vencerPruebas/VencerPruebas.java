package com.ensambla.backend.domain.useCase.plan.vencerPruebas;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Devuelve al gratuito los negocios cuya prueba terminó; cada negocio se vence en su propia transacción. */
@RequiredArgsConstructor
public class VencerPruebas {

    private final SuscripcionService suscripcionService;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        List<UUID> negocios = pruebaPremiumRepository.findNegociosVencidos(ahora);
        negocios.forEach(negocioId -> transaccionPort.ejecutar(() -> vencer(negocioId, ahora)));
    }

    private void vencer(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Suscripcion suscripcion = suscripcionService.obtener(negocioId);
        // Se vuelve a comprobar luego del bloqueo: un pago puede haberla activado desde la selección del job.
        if (suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && !suscripcion.getFinEn().isAfter(ahora)) {
            suscripcion.setPlanId(suscripcionService.obtenerPlan(CodigoPlan.GRATUITO).getId());
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioEn(ahora);
            suscripcion.setFinEn(null);
            suscripcion.setActualizadoEn(ahora);
            suscripcionRepository.save(suscripcion);
        }
    }
}
