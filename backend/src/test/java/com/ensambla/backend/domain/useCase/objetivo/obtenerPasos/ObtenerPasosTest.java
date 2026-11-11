package com.ensambla.backend.domain.useCase.objetivo.obtenerPasos;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerPasosTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private SistemaRepository sistemaRepository;

    @Mock
    private DatosDeProyectoService datosDeProyectoService;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private AgregadoZonaRepository agregadoZonaRepository;

    private ObtenerPasos obtenerPasos;

    @BeforeEach
    void setUp() {
        obtenerPasos = new ObtenerPasos(
                negocioRepository,
                sistemaRepository,
                datosDeProyectoService,
                zonaRepository,
                agregadoZonaRepository,
                new MetricasVentas(30, new BigDecimal("0.5")),
                CLOCK);
    }

    @Test
    @DisplayName("Carga en lote todos los sistemas del negocio de la sesión y devuelve una lista vacía")
    void executeShouldLoadEverySystemOfTheAuthenticatedBusiness() {
        Sistema primero = mock(Sistema.class);
        Sistema segundo = mock(Sistema.class);
        DatosDeProyecto datosPrimero = datosVacios("Primero");
        DatosDeProyecto datosSegundo = datosVacios("Segundo");
        givenBusiness();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(primero, segundo));
        when(datosDeProyectoService.cargar(primero)).thenReturn(datosPrimero);
        when(datosDeProyectoService.cargar(segundo)).thenReturn(datosSegundo);
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());

        ObtenerPasosResponse response = obtenerPasos.execute(new ObtenerPasosRequest(USUARIO_ID));

        assertThat(response.content()).isEmpty();
        verify(negocioRepository).obtenerIdDelUsuario(USUARIO_ID);
        verify(sistemaRepository).findAllByNegocioId(NEGOCIO_ID);
        verify(datosDeProyectoService).cargar(primero);
        verify(datosDeProyectoService).cargar(segundo);
        verifyNoInteractions(zonaRepository, agregadoZonaRepository);
    }

    @Test
    @DisplayName("Usa los agregados de la zona del negocio cuando tiene una asignada")
    void executeShouldUseBusinessZoneAggregatesWhenAssigned() {
        Zona zona = Zona.builder().id(ZONA_ID).nombre("San Justo").build();
        AnalisisZona analisis = new AnalisisZona("San Justo", 8, List.of());
        givenBusiness();
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);

        ObtenerPasosResponse response = obtenerPasos.execute(new ObtenerPasosRequest(USUARIO_ID));

        assertThat(response.content()).isEmpty();
        verify(agregadoZonaRepository).analizar(zona);
    }

    private void givenBusiness() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private static DatosDeProyecto datosVacios(String nombre) {
        return new DatosDeProyecto(nombre, List.of(), List.of(), List.of());
    }
}
