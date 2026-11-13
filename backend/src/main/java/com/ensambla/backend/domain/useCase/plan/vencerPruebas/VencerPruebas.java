package com.ensambla.backend.domain.useCase.plan.vencerPruebas;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Devuelve al gratuito las cuentas cuya prueba terminó; cada cuenta se vence en su propia transacción. */
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
        List<UsuarioId> cuentas = pruebaPremiumRepository.findUsuariosVencidos(ahora);
        cuentas.forEach(usuarioId -> transaccionPort.ejecutar(() -> vencer(usuarioId, ahora)));
    }

    private void vencer(UsuarioId usuarioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearCuenta(usuarioId);
        Suscripcion suscripcion = suscripcionRepository.obtener(usuarioId);
        // Se vuelve a comprobar luego del bloqueo: un pago puede haberla activado desde la selección del job.
        if (suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && !suscripcion.getFinEn().isAfter(ahora)) {
            suscripcion.setPlanId(planRepository.obtener(CodigoPlan.GRATUITO).getId());
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioEn(ahora);
            suscripcion.setFinEn(null);
            suscripcion.setActualizadoEn(ahora);
            suscripcionRepository.save(suscripcion);
        }
    }
}
