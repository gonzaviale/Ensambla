package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPrueba;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.domain.useCase.plan.vencerPruebas.VencerPruebas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Corre los casos de uso reales de la prueba premium contra PostgreSQL en el instante que elige cada test. El negocio
 * se indica directo (en producción sale de la sesión) y el vencimiento se limita a ese negocio para no tocar los
 * datos de otros tests.
 */
@Component
@RequiredArgsConstructor
public class PruebasPremiumDePrueba {

    private final PlanRepository planRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    public PruebaPremiumResponse iniciar(NegocioId negocioId, OffsetDateTime ahora) {
        return new IniciarPrueba(negocioPropio(negocioId), planRepository, pruebaPremiumRepository,
                suscripcionRepository, usoPlanRepository, renovacionPremiumRepository, transaccionPort,
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(new IniciarPruebaRequest(usuarioDe(negocioId)));
    }

    public void vencer(NegocioId negocioId, OffsetDateTime ahora) {
        PruebaPremiumRepository soloEsteNegocio = mock(PruebaPremiumRepository.class);
        when(soloEsteNegocio.findNegociosVencidos(any())).thenReturn(List.of(negocioId));
        new VencerPruebas(planRepository, soloEsteNegocio, suscripcionRepository, usoPlanRepository,
                transaccionPort, Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute();
    }

    /** El usuario de prueba comparte el UUID con su negocio. */
    private static UsuarioId usuarioDe(NegocioId negocioId) {
        return UsuarioId.de(negocioId.valor());
    }

    private static NegocioRepository negocioPropio(NegocioId negocioId) {
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(usuarioDe(negocioId))).thenReturn(negocioId);
        return negocioRepository;
    }
}
