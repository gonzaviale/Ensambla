package com.ensambla.backend.domain.useCase.plan.iniciarPrueba;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class IniciarPrueba {

    private final NegocioRepository negocioRepository;

    private final PlanRepository planRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public PruebaPremiumResponse execute(IniciarPruebaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return PruebaPremiumResponse.de(iniciarEnUnaTransaccion(negocioId, ahora));
    }

    private PruebaPremium iniciarEnUnaTransaccion(NegocioId negocioId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> iniciar(negocioId, ahora));
    }

    private PruebaPremium iniciar(NegocioId negocioId, OffsetDateTime ahora) {
        bloquearNegocio(negocioId);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        exigirQueNoHayaUsadoLaPruebaNiTengaUnaContratacionEnCurso(negocioId);
        Plan premium = obtenerPremiumConLimites();
        exigirQueNoTengaPremiumActivo(suscripcion, premium, ahora);
        PruebaPremium prueba = guardarPrueba(PruebaPremium.nueva(negocioId, ahora));
        pasarAPrueba(suscripcion, premium, prueba, ahora);
        return prueba;
    }

    private static void exigirQueNoTengaPremiumActivo(Suscripcion suscripcion, Plan premium, OffsetDateTime ahora) {
        if (suscripcion.tienePlanVigente(premium.getId(), ahora)) {
            throw new RestriccionPlanException("PREMIUM_ACTIVO", "Tu negocio ya tiene premium activo.");
        }
    }

    private void exigirQueNoHayaUsadoLaPruebaNiTengaUnaContratacionEnCurso(NegocioId negocioId) {
        if (pruebaPremiumRepository.findByNegocioId(negocioId).isPresent()) {
            throw new RestriccionPlanException("PRUEBA_UTILIZADA", "Este negocio ya utilizó su prueba premium.");
        }
        if (renovacionPremiumRepository.existeAbierta(negocioId)) {
            throw new RestriccionPlanException("RENOVACION_EN_CURSO",
                    "Tenés una contratación en curso. Cancelala antes de iniciar una prueba.");
        }
    }

    private Plan obtenerPremiumConLimites() {
        Plan premium = planRepository.obtener(CodigoPlan.PREMIUM);
        premium.validarLimitesConfigurados();
        return premium;
    }

    private void pasarAPrueba(Suscripcion suscripcion, Plan premium, PruebaPremium prueba, OffsetDateTime ahora) {
        suscripcion.pasarAPrueba(premium.getId(), prueba, ahora);
        suscripcionRepository.save(suscripcion);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Suscripcion obtenerSuscripcion(NegocioId negocioId) {
        return suscripcionRepository.obtener(negocioId);
    }

    private PruebaPremium guardarPrueba(PruebaPremium prueba) {
        return pruebaPremiumRepository.save(prueba);
    }
}
