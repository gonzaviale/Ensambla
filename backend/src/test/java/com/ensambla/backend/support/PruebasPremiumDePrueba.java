package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
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
 * se indica directo (en producción la cuenta sale de la sesión) y el vencimiento se limita a la cuenta de ese negocio
 * para no tocar los datos de otros tests.
 */
@Component
@RequiredArgsConstructor
public class PruebasPremiumDePrueba {

    private final NegocioRepository negocioRepository;

    private final PlanRepository planRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    public PruebaPremiumResponse iniciar(NegocioId negocioId, OffsetDateTime ahora) {
        return new IniciarPrueba(planRepository, pruebaPremiumRepository,
                suscripcionRepository, usoPlanRepository, renovacionPremiumRepository, transaccionPort,
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(new IniciarPruebaRequest(negocioRepository.obtenerDuenioId(negocioId)));
    }

    public void vencer(NegocioId negocioId, OffsetDateTime ahora) {
        PruebaPremiumRepository soloEstaCuenta = mock(PruebaPremiumRepository.class);
        when(soloEstaCuenta.findUsuariosVencidos(any())).thenReturn(List.of(negocioRepository.obtenerDuenioId(negocioId)));
        new VencerPruebas(planRepository, soloEstaCuenta, suscripcionRepository, usoPlanRepository,
                transaccionPort, Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute();
    }
}
