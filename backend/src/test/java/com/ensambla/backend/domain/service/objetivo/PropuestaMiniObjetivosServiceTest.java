package com.ensambla.backend.domain.service.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PropuestaMiniObjetivosServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private PropuestaMiniObjetivosService service;

    @BeforeEach
    void setUp() {
        service = new PropuestaMiniObjetivosService(
                negocioRepository,
                sistemaRepository,
                datosDeProyectoService,
                zonaRepository,
                agregadoZonaRepository,
                new MetricasVentas(30, new BigDecimal("0.5")));
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Calcula el resultado y los candidatos con los datos de todos los sistemas del negocio")
    void candidatosShouldUseTheDataOfEverySystemOfTheBusiness() {
        givenSistemasConDatos(almacen(), new DatosDeProyecto("Kiosco", List.of(), List.of(), List.of()));

        CandidatosDelNegocio result = service.candidatos(NEGOCIO_ID, HOY);

        assertThat(result.resultado()).isEqualByComparingTo("-27000");
        assertThat(result.candidatos()).extracting(CandidatoMiniObjetivo::tipo).contains(
                TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES, TipoMiniObjetivo.SUBIR_PRECIO);
        verifyNoInteractions(zonaRepository, agregadoZonaRepository);
    }

    @Test
    @DisplayName("El medidor carga los datos de todos los sistemas del negocio")
    void medidorShouldLoadTheDataOfEverySystemOfTheBusiness() {
        givenSistemasConDatos(almacen(), new DatosDeProyecto("Kiosco", List.of(), List.of(), List.of()));

        MedidorMiniObjetivo result = service.medidor(NEGOCIO_ID, HOY);

        assertThat(result.hayVentas()).isTrue();
        assertThat(result.resultado()).isEqualByComparingTo("-27000");
    }

    @Test
    @DisplayName("Los candidatos de un medidor ya cargado salen sin volver a leer los sistemas")
    void candidatosShouldUseAnAlreadyLoadedMeter() {
        givenSistemasConDatos(almacen());
        MedidorMiniObjetivo medidor = service.medidor(NEGOCIO_ID, HOY);

        List<CandidatoMiniObjetivo> result = service.candidatos(NEGOCIO_ID, medidor);

        assertThat(result).extracting(CandidatoMiniObjetivo::tipo).contains(TipoMiniObjetivo.PONER_A_LA_VISTA);
        verify(sistemaRepository, times(1)).findAllByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Sin sistemas no hay resultado ni candidatos")
    void candidatosShouldBeEmptyWhenTheBusinessHasNoSystems() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());

        CandidatosDelNegocio result = service.candidatos(NEGOCIO_ID, HOY);

        assertThat(result.resultado()).isEqualByComparingTo("0");
        assertThat(result.candidatos()).isEmpty();
    }

    @Test
    @DisplayName("Con zona asignada no sube el precio por encima del máximo que se cobra en la zona")
    void candidatosShouldUseTheZoneOfTheBusiness() {
        Zona zona = Zona.builder().id(ZONA_ID).nombre("San Justo").build();
        AnalisisZona analisis = new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto("Yerba 1 kg", 5, 5,
                new BigDecimal("3900"), new BigDecimal("4100"), new BigDecimal("4300"), BigDecimal.TEN)));
        givenSistemasConDatos(almacen());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);

        CandidatosDelNegocio result = service.candidatos(NEGOCIO_ID, HOY);

        verify(agregadoZonaRepository).analizar(zona);
        assertThat(result.candidatos())
                .filteredOn(candidato -> candidato.tipo() == TipoMiniObjetivo.SUBIR_PRECIO)
                .singleElement()
                .satisfies(candidato -> assertThat(candidato.titulo()).isEqualTo("Subile $100 a Yerba 1 kg"));
    }

    // --- arrange ---
    private void givenSistemasConDatos(DatosDeProyecto... datos) {
        List<Sistema> sistemas = new ArrayList<>();
        for (DatosDeProyecto dato : datos) {
            Sistema sistema = mock(Sistema.class);
            when(datosDeProyectoService.cargar(sistema)).thenReturn(dato);
            sistemas.add(sistema);
        }
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(sistemas);
    }
}
