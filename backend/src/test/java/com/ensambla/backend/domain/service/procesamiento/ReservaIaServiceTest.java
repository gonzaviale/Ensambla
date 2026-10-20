package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.lote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReservaIaServiceTest {

    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class);
    private final ReservaIaService reservaIaService = new ReservaIaService(
            controlPlanService, procesamientoRepository);

    @Test
    @DisplayName("La cuota mensual se valida antes de reservar páginas en el día")
    void reservarShouldValidateMonthBeforeDailyReservation() {
        var lote = lote(1);
        when(procesamientoRepository.reservar(lote, 2, 100, AHORA)).thenReturn(true);

        boolean result = reservaIaService.reservar(lote, 2, 100, AHORA);

        assertThat(result).isTrue();
        var orden = inOrder(controlPlanService, procesamientoRepository);
        orden.verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 2, AHORA);
        orden.verify(procesamientoRepository).reservar(lote, 2, 100, AHORA);
    }

    @Test
    @DisplayName("Un rechazo mensual no crea una reserva diaria")
    void reservarShouldNotReserveWhenMonthlyQuotaIsFull() {
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Sin páginas"))
                .when(controlPlanService).validarPaginasIa(NEGOCIO_ID, 2, AHORA);

        assertThatThrownBy(() -> reservaIaService.reservar(lote(1), 2, 100, AHORA))
                .isInstanceOf(RestriccionPlanException.class);

        verifyNoInteractions(procesamientoRepository);
    }

    @Test
    @DisplayName("El reintento exige que quede al menos una página mensual")
    void validarDisponibilidadShouldCheckOnePage() {
        reservaIaService.validarDisponibilidad(NEGOCIO_ID, AHORA);

        verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
    }
}
