package com.ensambla.backend.domain.useCase.plan.vencerPruebas;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Devuelve al gratuito los negocios cuya prueba terminó; cada negocio se vence en su propia transacción. */
@RequiredArgsConstructor
public class VencerPruebas {

    private final PlanRepository planRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        buscarNegociosConPruebaVencida(ahora).forEach(negocioId -> vencerEnUnaTransaccion(negocioId, ahora));
    }

    private void vencerEnUnaTransaccion(NegocioId negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> vencer(negocioId, ahora));
    }

    private void vencer(NegocioId negocioId, OffsetDateTime ahora) {
        bloquearNegocio(negocioId);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        // Se vuelve a comprobar luego del bloqueo: un pago puede haberla activado desde la selección del job.
        if (suscripcion.tieneLaPruebaVencida(ahora)) {
            volverAlPlanGratuito(suscripcion, ahora);
        }
    }

    private void volverAlPlanGratuito(Suscripcion suscripcion, OffsetDateTime ahora) {
        suscripcion.volverAlPlanGratuito(obtenerPlanGratuito().getId(), ahora);
        suscripcionRepository.save(suscripcion);
    }

    private List<NegocioId> buscarNegociosConPruebaVencida(OffsetDateTime ahora) {
        return pruebaPremiumRepository.findNegociosVencidos(ahora);
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Suscripcion obtenerSuscripcion(NegocioId negocioId) {
        return suscripcionRepository.obtener(negocioId);
    }

    private Plan obtenerPlanGratuito() {
        return planRepository.obtener(CodigoPlan.GRATUITO);
    }
}
