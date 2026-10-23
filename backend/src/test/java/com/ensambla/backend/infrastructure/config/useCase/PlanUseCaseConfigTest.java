package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.dto.plan.IniciarPruebaRequest;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.PruebaPremiumService;
import com.ensambla.backend.domain.service.plan.ConsultaPlanesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.ConsultaPlanesTestData.consulta;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanUseCaseConfigTest {

    private final PlanUseCaseConfig planUseCaseConfig = new PlanUseCaseConfig();
    private final Clock clock = Clock.fixed(INSTANTE.toInstant(), ZoneOffset.UTC);

    @Test
    @DisplayName("El wiring conecta la consulta con usuario, reloj y mapper real")
    void consultarPlanesUseCaseShouldWireDependencies() {
        NegocioService negocioService = mock(NegocioService.class);
        ConsultaPlanesService consultaPlanesService = mock(ConsultaPlanesService.class);
        when(negocioService.obtenerDelUsuario(NEGOCIO_ID)).thenReturn(NEGOCIO_ID);
        when(consultaPlanesService.consultar(NEGOCIO_ID, INSTANTE))
                .thenReturn(consulta(false));

        var result = planUseCaseConfig.consultarPlanesUseCase(negocioService, consultaPlanesService,
                planUseCaseConfig.consultarPlanesMapper(), clock).execute(NEGOCIO_ID);

        assertThat(result.consultadoEn()).isEqualTo(INSTANTE);
        assertThat(result.planes()).hasSize(2);
    }

    @Test
    @DisplayName("El wiring conecta usuario, servicio, mapper y reloj al inicio de prueba")
    void iniciarPruebaUseCaseShouldWireDependencies() {
        NegocioService negocioService = mock(NegocioService.class);
        PruebaPremiumService pruebaPremiumService = mock(PruebaPremiumService.class);
        when(negocioService.obtenerDelUsuario(NEGOCIO_ID)).thenReturn(NEGOCIO_ID);
        when(pruebaPremiumService.iniciar(NEGOCIO_ID, INSTANTE))
                .thenReturn(PruebaPremium.nueva(NEGOCIO_ID, INSTANTE));

        var result = planUseCaseConfig.iniciarPruebaUseCase(negocioService, pruebaPremiumService,
                planUseCaseConfig.iniciarPruebaMapper(), clock).execute(new IniciarPruebaRequest(NEGOCIO_ID));

        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.limitePaginasIa()).isEqualTo(10);
    }

    @Test
    @DisplayName("El wiring conecta reloj y servicio al vencimiento automático")
    void vencerPruebasUseCaseShouldWireDependencies() {
        PruebaPremiumService pruebaPremiumService = mock(PruebaPremiumService.class);
        when(pruebaPremiumService.listarVencidas(INSTANTE)).thenReturn(List.of(NEGOCIO_ID));

        planUseCaseConfig.vencerPruebasUseCase(pruebaPremiumService, clock).execute();

        verify(pruebaPremiumService).vencer(NEGOCIO_ID, INSTANTE);
    }
}
