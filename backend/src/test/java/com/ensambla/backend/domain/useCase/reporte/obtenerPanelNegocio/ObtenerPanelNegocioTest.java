package com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObtenerPanelNegocioTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000002");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private final ObtenerPanelNegocio obtenerPanelNegocio = new ObtenerPanelNegocio(
            negocioRepository, proyectoRepository, datosDeProyectoService, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    private ObtenerPanelNegocioResponse respuesta;
    private Throwable excepcion;
    private Throwable otraExcepcion;

    @BeforeEach
    void setUp() {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Almacén").build();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(new DatosDeProyecto("Almacén", List.of(), List.of(), List.of()));
    }

    @Test
    @DisplayName("Sin días mira los últimos 30 hasta hoy en hora argentina")
    void sinDiasMiraLosUltimos30HastaHoyEnHoraArgentina() {
        cuandoSePideElPanelDe(null);

        entoncesMiraDesdeElHastaConLaCantidadDeDias(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 19), 30);
    }

    @Test
    @DisplayName("Mira los últimos días que pidió la persona")
    void miraLosUltimosDiasQuePidioLaPersona() {
        cuandoSePideElPanelDe(7);

        entoncesMiraLosUltimosDias(LocalDate.of(2026, 10, 13), 7);
    }

    @Test
    @DisplayName("Un sistema sin ventas responde vacío, sin métricas ni semanas")
    void unSistemaSinVentasRespondeVacioSinMetricasNiSemanas() {
        cuandoSePideElPanelDe(null);

        entoncesRespondeVacioSinMetricasNiSemanas();
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void unSistemaDeOtroNegocioRespondeComoInexistente() {
        dadoUnSistemaDeOtroNegocio();

        cuandoSeIntentaPedirElPanelDelSistemaAjeno();

        entoncesSeRechazaComoInexistenteSinCargarLosDatos();
    }

    @Test
    @DisplayName("Rechaza una cantidad de días inválida sin cargar los datos")
    void rechazaUnaCantidadDeDiasInvalidaSinCargarLosDatos() {
        cuandoSeIntentaPedirElPanelConCeroYConTrescientosSesentaYSieteDias();

        entoncesSeRechazanLosDosPedidosSinCargarLosDatos();
    }

    // --- dado ---
    private void dadoUnSistemaDeOtroNegocio() {
        when(proyectoRepository.obtener(NEGOCIO_ID, OTRO_PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    // --- cuando ---
    private void cuandoSePideElPanelDe(Integer dias) {
        respuesta = obtenerPanelNegocio.execute(new ObtenerPanelNegocioRequest(USUARIO_ID, PROYECTO_ID, dias));
    }

    private void cuandoSeIntentaPedirElPanelDelSistemaAjeno() {
        excepcion = catchThrowable(() -> obtenerPanelNegocio.execute(
                new ObtenerPanelNegocioRequest(USUARIO_ID, OTRO_PROYECTO_ID, null)));
    }

    private void cuandoSeIntentaPedirElPanelConCeroYConTrescientosSesentaYSieteDias() {
        excepcion = catchThrowable(() -> cuandoSePideElPanelDe(0));
        otraExcepcion = catchThrowable(() -> cuandoSePideElPanelDe(367));
    }

    // --- entonces ---
    private void entoncesMiraDesdeElHastaConLaCantidadDeDias(LocalDate desde, LocalDate hasta, int dias) {
        assertThat(respuesta.desde()).isEqualTo(desde);
        assertThat(respuesta.hasta()).isEqualTo(hasta);
        assertThat(respuesta.dias()).isEqualTo(dias);
    }

    private void entoncesMiraLosUltimosDias(LocalDate desde, int dias) {
        assertThat(respuesta.desde()).isEqualTo(desde);
        assertThat(respuesta.dias()).isEqualTo(dias);
    }

    private void entoncesRespondeVacioSinMetricasNiSemanas() {
        assertThat(respuesta.hayDatos()).isFalse();
        assertThat(respuesta.metricas()).isNull();
        assertThat(respuesta.flujoDeCaja()).isEmpty();
    }

    private void entoncesSeRechazaComoInexistenteSinCargarLosDatos() {
        assertThat(excepcion).isInstanceOf(ResourceNotFoundException.class);
        verify(datosDeProyectoService, never()).cargar(any(), any());
    }

    private void entoncesSeRechazanLosDosPedidosSinCargarLosDatos() {
        assertThat(excepcion).isInstanceOf(BusinessRuleException.class);
        assertThat(otraExcepcion).isInstanceOf(BusinessRuleException.class);
        verify(datosDeProyectoService, never()).cargar(any(), any());
    }
}
