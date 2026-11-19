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

    private Zona zona;
    private CandidatosDelNegocio candidatos;
    private MedidorMiniObjetivo medidor;
    private List<CandidatoMiniObjetivo> candidatosDelMedidor;

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
    void calculaElResultadoYLosCandidatosConLosDatosDeTodosLosSistemasDelNegocio() {
        dadoUnNegocioConUnAlmacenYUnKioscoSinDatos();

        cuandoSePidenLosCandidatosDelNegocio();

        entoncesElResultadoEsDeMenos27000ConLosCandidatosHabituales();
        entoncesNoSeConsultaLaZona();
    }

    @Test
    @DisplayName("El medidor carga los datos de todos los sistemas del negocio")
    void elMedidorCargaLosDatosDeTodosLosSistemasDelNegocio() {
        dadoUnNegocioConUnAlmacenYUnKioscoSinDatos();

        cuandoSePideElMedidor();

        entoncesElMedidorTieneVentasYResultadoDeMenos27000();
    }

    @Test
    @DisplayName("Los candidatos de un medidor ya cargado salen sin volver a leer los sistemas")
    void losCandidatosDeUnMedidorYaCargadoSalenSinVolverALeerLosSistemas() {
        dadoUnNegocioConUnAlmacen();

        cuandoSePidenLosCandidatosDeUnMedidorYaCargado();

        entoncesSeProponePonerAlaVistaYLosSistemasSeLeyeronUnaSolaVez();
    }

    @Test
    @DisplayName("Sin sistemas no hay resultado ni candidatos")
    void sinSistemasNoHayResultadoNiCandidatos() {
        dadoUnNegocioSinSistemas();

        cuandoSePidenLosCandidatosDelNegocio();

        entoncesNoHayResultadoNiCandidatos();
    }

    @Test
    @DisplayName("Con zona asignada no sube el precio por encima del máximo que se cobra en la zona")
    void conZonaAsignadaNoSubeElPrecioPorEncimaDelMaximoQueSeCobraEnLaZona() {
        dadoUnNegocioConUnAlmacen();
        dadoUnaZonaAsignadaDondeLaYerbaSeCobraHasta4300();

        cuandoSePidenLosCandidatosDelNegocio();

        entoncesSeAnalizaLaZonaYSeSugiereSubirCienPesosLaYerba();
    }

    // --- dado ---

    private void dadoUnNegocioConUnAlmacenYUnKioscoSinDatos() {
        dadoLosSistemasConDatos(almacen(), new DatosDeProyecto("Kiosco", List.of(), List.of(), List.of()));
    }

    private void dadoUnNegocioConUnAlmacen() {
        dadoLosSistemasConDatos(almacen());
    }

    private void dadoUnNegocioSinSistemas() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnaZonaAsignadaDondeLaYerbaSeCobraHasta4300() {
        zona = Zona.builder().id(ZONA_ID).nombre("San Justo").build();
        AnalisisZona analisis = new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto("Yerba 1 kg", 5, 5,
                new BigDecimal("3900"), new BigDecimal("4100"), new BigDecimal("4300"), BigDecimal.TEN)));
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);
    }

    private void dadoLosSistemasConDatos(DatosDeProyecto... datos) {
        List<Sistema> sistemas = new ArrayList<>();
        for (DatosDeProyecto dato : datos) {
            Sistema sistema = mock(Sistema.class);
            when(datosDeProyectoService.cargar(sistema)).thenReturn(dato);
            sistemas.add(sistema);
        }
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(sistemas);
    }

    // --- cuando ---

    private void cuandoSePidenLosCandidatosDelNegocio() {
        candidatos = service.candidatos(NEGOCIO_ID, HOY);
    }

    private void cuandoSePideElMedidor() {
        medidor = service.medidor(NEGOCIO_ID, HOY);
    }

    private void cuandoSePidenLosCandidatosDeUnMedidorYaCargado() {
        cuandoSePideElMedidor();
        candidatosDelMedidor = service.candidatos(NEGOCIO_ID, medidor);
    }

    // --- entonces ---

    private void entoncesElResultadoEsDeMenos27000ConLosCandidatosHabituales() {
        assertThat(candidatos.resultado()).isEqualByComparingTo("-27000");
        assertThat(candidatos.candidatos()).extracting(CandidatoMiniObjetivo::tipo).contains(
                TipoMiniObjetivo.PONER_A_LA_VISTA, TipoMiniObjetivo.VENTAS_SEMANALES, TipoMiniObjetivo.SUBIR_PRECIO);
    }

    private void entoncesNoSeConsultaLaZona() {
        verifyNoInteractions(zonaRepository, agregadoZonaRepository);
    }

    private void entoncesElMedidorTieneVentasYResultadoDeMenos27000() {
        assertThat(medidor.hayVentas()).isTrue();
        assertThat(medidor.resultado()).isEqualByComparingTo("-27000");
    }

    private void entoncesSeProponePonerAlaVistaYLosSistemasSeLeyeronUnaSolaVez() {
        assertThat(candidatosDelMedidor).extracting(CandidatoMiniObjetivo::tipo)
                .contains(TipoMiniObjetivo.PONER_A_LA_VISTA);
        verify(sistemaRepository, times(1)).findAllByNegocioId(NEGOCIO_ID);
    }

    private void entoncesNoHayResultadoNiCandidatos() {
        assertThat(candidatos.resultado()).isEqualByComparingTo("0");
        assertThat(candidatos.candidatos()).isEmpty();
    }

    private void entoncesSeAnalizaLaZonaYSeSugiereSubirCienPesosLaYerba() {
        verify(agregadoZonaRepository).analizar(zona);
        assertThat(candidatos.candidatos())
                .filteredOn(candidato -> candidato.tipo() == TipoMiniObjetivo.SUBIR_PRECIO)
                .singleElement()
                .satisfies(candidato -> assertThat(candidato.titulo()).isEqualTo("Subile $100 a Yerba 1 kg"));
    }
}
