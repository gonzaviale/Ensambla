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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void executeShouldUseTheLastThirtyDaysByDefault() {
        ObtenerPanelNegocioResponse response = whenObtener(null);

        assertThat(response.desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.hasta()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(response.dias()).isEqualTo(30);
    }

    @Test
    @DisplayName("Mira los últimos días que pidió la persona")
    void executeShouldUseTheRequestedDays() {
        ObtenerPanelNegocioResponse response = whenObtener(7);

        assertThat(response.desde()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(response.dias()).isEqualTo(7);
    }

    @Test
    @DisplayName("Un sistema sin ventas responde vacío, sin métricas ni semanas")
    void executeShouldRespondEmptyWhenThereAreNoSales() {
        ObtenerPanelNegocioResponse response = whenObtener(null);

        assertThat(response.hayDatos()).isFalse();
        assertThat(response.metricas()).isNull();
        assertThat(response.flujoDeCaja()).isEmpty();
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, OTRO_PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> obtenerPanelNegocio.execute(
                new ObtenerPanelNegocioRequest(USUARIO_ID, OTRO_PROYECTO_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(datosDeProyectoService, never()).cargar(any(), any());
    }

    @Test
    @DisplayName("Rechaza una cantidad de días inválida sin cargar los datos")
    void executeShouldRejectInvalidDays() {
        assertThatThrownBy(() -> whenObtener(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> whenObtener(367)).isInstanceOf(BusinessRuleException.class);
        verify(datosDeProyectoService, never()).cargar(any(), any());
    }

    private ObtenerPanelNegocioResponse whenObtener(Integer dias) {
        return obtenerPanelNegocio.execute(new ObtenerPanelNegocioRequest(USUARIO_ID, PROYECTO_ID, dias));
    }
}
