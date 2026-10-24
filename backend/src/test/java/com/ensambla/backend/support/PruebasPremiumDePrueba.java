package com.ensambla.backend.support;

import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
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

    private final SuscripcionService suscripcionService;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    public PruebaPremiumResponse iniciar(UUID negocioId, OffsetDateTime ahora) {
        return new IniciarPrueba(negocioPropio(negocioId), suscripcionService, pruebaPremiumRepository,
                suscripcionRepository, usoPlanRepository, renovacionPremiumRepository, transaccionPort,
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(new IniciarPruebaRequest(negocioId));
    }

    public void vencer(UUID negocioId, OffsetDateTime ahora) {
        PruebaPremiumRepository soloEsteNegocio = mock(PruebaPremiumRepository.class);
        when(soloEsteNegocio.findNegociosVencidos(any())).thenReturn(List.of(negocioId));
        new VencerPruebas(suscripcionService, soloEsteNegocio, suscripcionRepository, usoPlanRepository,
                transaccionPort, Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute();
    }

    private static NegocioService negocioPropio(UUID negocioId) {
        NegocioService negocioService = mock(NegocioService.class);
        when(negocioService.obtenerDelUsuario(negocioId)).thenReturn(negocioId);
        return negocioService;
    }
}
