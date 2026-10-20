package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumoIaInmediatoServiceTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-30T23:30:00-03:00");
    private static final LocalDate DIA = LocalDate.parse("2026-10-01");

    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final ConsumoIaInmediatoService consumoIaInmediatoService = new ConsumoIaInmediatoService(
            controlPlanService, usoPlanRepository, ProcesamientoTestData.politica());

    @Test
    @DisplayName("Valida el mes y registra una página en el día UTC antes de invocar IA")
    void registrarShouldCheckMonthBeforeChargingUtcDay() {
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, DIA, 100, AHORA)).thenReturn(true);

        consumoIaInmediatoService.registrar(NEGOCIO_ID, AHORA);

        var orden = inOrder(controlPlanService, usoPlanRepository);
        orden.verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
        orden.verify(usoPlanRepository).registrarPaginaInmediata(NEGOCIO_ID, DIA, 100, AHORA);
    }

    @Test
    @DisplayName("Devuelve una restricción clara cuando se agota el límite diario")
    void registrarShouldRejectDailyLimit() {
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, DIA, 100, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> consumoIaInmediatoService.registrar(NEGOCIO_ID, AHORA))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Se alcanzó el límite diario de páginas de IA. Probá mañana.");
    }
}
