package com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProcesamientoTestData.AHORA;
import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReintentarLecturaTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("90000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ProcesamientoRepository procesamientoRepository;

    @Mock
    private ControlPlanService controlPlanService;

    private ReintentarLectura reintentarLectura;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        reintentarLectura = new ReintentarLectura(negocioRepository, controlPlanService, procesamientoRepository,
                ProcesamientoTestData.politica(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Reencola un lote propio cuando hay disponibilidad")
    void reencolaUnLotePropioCuandoHayDisponibilidad() {
        dadoUnLoteEnErrorConConsumoDelDia(5, 2);
        dadoQueElLoteSePuedeReencolar(true);

        cuandoSeReintentaLaLectura();

        entoncesSeExigioUnaPaginaDelPlanYSeReencolo();
    }

    @Test
    @DisplayName("Rechaza reintentar si el plan no deja al menos una página en el mes")
    void rechazaReintentarSiElPlanNoDejaAlMenosUnaPaginaEnElMes() {
        dadoUnLoteEnError();
        dadoQueElPlanNoTienePaginasEnElMes();

        cuandoSeIntentaReintentarLaLectura();

        entoncesSeRechazaPorRestriccionDelPlanSinReencolar();
    }

    @Test
    @DisplayName("Rechaza reintentar si no queda disponibilidad diaria")
    void rechazaReintentarSiNoQuedaDisponibilidadDiaria() {
        dadoUnLoteEnErrorConConsumoDelDia(90, 10);

        cuandoSeIntentaReintentarLaLectura();

        entoncesSeRechazaPorLimiteDiarioSinReencolar();
    }

    @Test
    @DisplayName("Rechaza reintentar un estado distinto a ERROR")
    void rechazaReintentarUnEstadoDistintoAError() {
        dadoUnLoteEnErrorConConsumoDelDia(0, 0);
        dadoQueElLoteSePuedeReencolar(false);

        cuandoSeIntentaReintentarLaLectura();

        entoncesSeRechazaPorEstadoQueNoEsError();
    }

    // --- dado ---
    private void dadoUnLoteEnError() {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(unEstadoEnError()));
    }

    private void dadoUnLoteEnErrorConConsumoDelDia(int paginas, int reservadas) {
        dadoUnLoteEnError();
        when(procesamientoRepository.consumo(NEGOCIO_ID, AHORA.toLocalDate()))
                .thenReturn(new ConsumoDiario(AHORA.toLocalDate(), paginas, reservadas, 0, 0, null, false));
    }

    private void dadoQueElPlanNoTienePaginasEnElMes() {
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Sin páginas"))
                .when(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
    }

    private void dadoQueElLoteSePuedeReencolar(boolean sePuede) {
        when(procesamientoRepository.reencolar(NEGOCIO_ID, LOTE_ID, AHORA)).thenReturn(sePuede);
    }

    private static EstadoLectura unEstadoEnError() {
        return new EstadoLectura(LOTE_ID, EstadoLote.ERROR, "Error", 3, null);
    }

    // --- cuando ---
    private void cuandoSeReintentaLaLectura() {
        reintentarLectura.execute(new UsuarioLoteRequest(USUARIO_ID, LOTE_ID));
    }

    private void cuandoSeIntentaReintentarLaLectura() {
        rechazo = catchThrowable(this::cuandoSeReintentaLaLectura);
    }

    // --- entonces ---
    private void entoncesSeExigioUnaPaginaDelPlanYSeReencolo() {
        verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 1, AHORA);
        verify(procesamientoRepository).reencolar(NEGOCIO_ID, LOTE_ID, AHORA);
    }

    private void entoncesSeRechazaPorRestriccionDelPlanSinReencolar() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class);
        verify(procesamientoRepository, never()).reencolar(any(), any(), any());
    }

    private void entoncesSeRechazaPorLimiteDiarioSinReencolar() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        verify(procesamientoRepository, never()).reencolar(any(), any(), any());
    }

    private void entoncesSeRechazaPorEstadoQueNoEsError() {
        assertThat(rechazo).hasMessageContaining("estado ERROR");
    }
}
