package com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReintentarLecturaTest {

    private static final UUID USUARIO_ID = UUID.fromString("90000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioService negocioService;

    @Mock
    private ProcesamientoRepository procesamientoRepository;

    @Mock
    private ControlPlanService controlPlanService;

    private ReintentarLectura reintentarLectura;

    @BeforeEach
    void setUp() {
        reintentarLectura = new ReintentarLectura(negocioService, new ProcesamientoService(procesamientoRepository),
                controlPlanService, procesamientoRepository, ProcesamientoTestData.politica(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Reencola un lote propio cuando hay disponibilidad")
    void executeShouldRequeueOwnError() {
        givenRetry(5, 2);
        when(procesamientoRepository.reencolar(NEGOCIO_ID, LOTE_ID, AHORA)).thenReturn(true);

        reintentarLectura.execute(request());

        verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
        verify(procesamientoRepository).reencolar(NEGOCIO_ID, LOTE_ID, AHORA);
    }

    @Test
    @DisplayName("Rechaza reintentar si el plan no deja al menos una página en el mes")
    void executeShouldRejectWhenMonthlyQuotaIsFull() {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(givenEstado()));
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Sin páginas"))
                .when(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
        UsuarioLoteRequest request = request();

        assertThatThrownBy(() -> reintentarLectura.execute(request)).isInstanceOf(RestriccionPlanException.class);
        verify(procesamientoRepository, never()).reencolar(any(), any(), any());
    }

    @Test
    @DisplayName("Rechaza reintentar si no queda disponibilidad diaria")
    void executeShouldRejectExhaustedQuota() {
        givenRetry(90, 10);
        UsuarioLoteRequest request = request();

        assertThatThrownBy(() -> reintentarLectura.execute(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        verify(procesamientoRepository, never()).reencolar(any(), any(), any());
    }

    @Test
    @DisplayName("Rechaza reintentar un estado distinto a ERROR")
    void executeShouldRejectWrongState() {
        givenRetry(0, 0);
        when(procesamientoRepository.reencolar(NEGOCIO_ID, LOTE_ID, AHORA)).thenReturn(false);
        UsuarioLoteRequest request = request();

        assertThatThrownBy(() -> reintentarLectura.execute(request)).hasMessageContaining("estado ERROR");
    }

    // --- arrange ---
    private void givenRetry(int paginas, int reservadas) {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(givenEstado()));
        when(procesamientoRepository.consumo(NEGOCIO_ID, AHORA.toLocalDate()))
                .thenReturn(new ConsumoDiario(AHORA.toLocalDate(), paginas, reservadas, 0, 0, null, false));
    }

    private static EstadoLectura givenEstado() {
        return new EstadoLectura(LOTE_ID, EstadoLote.ERROR, "Error", 3, null);
    }

    // --- helpers ---
    private static UsuarioLoteRequest request() {
        return new UsuarioLoteRequest(USUARIO_ID, LOTE_ID);
    }
}
