package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class IniciarPrueba {

    private final NegocioService negocioService;

    private final SuscripcionService suscripcionService;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public PruebaPremiumResponse execute(IniciarPruebaRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return PruebaPremiumResponse.de(transaccionPort.ejecutar(() -> iniciar(negocioId, ahora)));
    }

    private PruebaPremium iniciar(NegocioId negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Suscripcion suscripcion = suscripcionService.obtener(negocioId);
        validarSinPruebaNiContratacion(negocioId);
        Plan premium = suscripcionService.obtenerPlan(CodigoPlan.PREMIUM);
        premium.validarLimitesConfigurados();
        if (suscripcion.estaVigente(ahora) && premium.getId().equals(suscripcion.getPlanId())) {
            throw new RestriccionPlanException("PREMIUM_ACTIVO", "Tu negocio ya tiene premium activo.");
        }
        PruebaPremium prueba = pruebaPremiumRepository.save(PruebaPremium.nueva(negocioId, ahora));
        pasarAPrueba(suscripcion, premium, prueba, ahora);
        return prueba;
    }

    private void validarSinPruebaNiContratacion(NegocioId negocioId) {
        if (pruebaPremiumRepository.findByNegocioId(negocioId).isPresent()) {
            throw new RestriccionPlanException("PRUEBA_UTILIZADA", "Este negocio ya utilizó su prueba premium.");
        }
        if (renovacionPremiumRepository.existeAbierta(negocioId)) {
            throw new RestriccionPlanException("RENOVACION_EN_CURSO",
                    "Tenés una contratación en curso. Cancelala antes de iniciar una prueba.");
        }
    }

    private void pasarAPrueba(Suscripcion suscripcion, Plan premium, PruebaPremium prueba, OffsetDateTime ahora) {
        suscripcion.setPlanId(premium.getId());
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setInicioEn(prueba.inicioEn());
        suscripcion.setFinEn(prueba.finEn());
        suscripcion.setActualizadoEn(ahora);
        suscripcionRepository.save(suscripcion);
    }
}
