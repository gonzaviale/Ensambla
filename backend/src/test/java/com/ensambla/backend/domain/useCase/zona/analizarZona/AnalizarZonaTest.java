package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ZonasDePrueba.LATITUD;
import static com.ensambla.backend.support.ZonasDePrueba.LONGITUD;
import static com.ensambla.backend.support.ZonasDePrueba.NOMBRE_ZONA;
import static com.ensambla.backend.support.ZonasDePrueba.USUARIO_ID;
import static com.ensambla.backend.support.ZonasDePrueba.autorizaSoloLaPrimera;
import static com.ensambla.backend.support.ZonasDePrueba.autorizaTodo;
import static com.ensambla.backend.support.ZonasDePrueba.unAnalisisDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unProductoDeZona;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaDeSanJusto;
import static com.ensambla.backend.support.ZonasDePrueba.unaZonaDeVillaCrespo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalizarZonaTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final String MENSAJE_SIN_DATOS = "Todavía no tenemos datos de esa zona";
    private static final String DIRECCION = "Arieta 3120, San Justo";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final GeocodificadorPort geocodificadorPort = mock(GeocodificadorPort.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final Zona zona = unaZonaDeSanJusto();

    private ControlPlanService controlPlanService = mock(ControlPlanService.class, autorizaTodo());
    private ZonaItem zonaItem;
    private Throwable rechazo;

    @Test
    @DisplayName("Resuelve la zona del punto del mapa sin buscar el texto ni la dirección")
    void resuelveLaZonaDelPuntoDelMapaSinBuscarElTextoNiLaDireccion() {
        dadoUnPuntoDentroDeLaZonaConUnaYerba();

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesSeInformaLaZonaConLosDatosDeLaYerba();
        entoncesNoSeConsultaElGeocodificador();
    }

    @Test
    @DisplayName("Con punto y lugar a la vez manda el punto")
    void conPuntoYLugarALaVezMandaElPunto() {
        dadoUnPuntoDentroDeLaZonaConUnaYerba();

        cuandoSeAnalizaElPuntoDelMapaConElLugar("Palermo");

        entoncesSeResuelveLaZonaDelPunto();
        entoncesNoSeConsultaElGeocodificador();
    }

    @Test
    @DisplayName("Falla con 404 si el punto cae fuera de toda zona")
    void unPuntoFueraDeTodaZonaSeInformaComoSinDatos() {
        dadoUnPuntoFueraDeTodaZona();

        cuandoSeIntentaAnalizarElPuntoDelMapa();

        entoncesSeRechazaPorZonaSinDatos();
        entoncesNoSeAnalizaNingunaZona();
    }

    @Test
    @DisplayName("Encuentra el primer barrio cuyo nombre o localidad coincide, sin consultar el mapa")
    void encuentraElBarrioPorSuNombreSinConsultarElMapa() {
        dadoQueElBarrioEsConocidoYTieneUnaYerba();

        cuandoSeAnalizaElLugar("san justo");

        entoncesSeInformaLaZonaDeSanJusto();
        entoncesNoSeConsultaElGeocodificador();
    }

    @Test
    @DisplayName("Resuelve la zona de una dirección cuando no es un barrio conocido")
    void resuelveLaZonaDeUnaDireccionCuandoNoEsUnBarrioConocido() {
        dadoUnaDireccionQueCaeDentroDeLaZona();

        cuandoSeAnalizaElLugar(DIRECCION);

        entoncesSeInformanDiezComercios();
    }

    @Test
    @DisplayName("Falla con 404 si la dirección no se encuentra")
    void unaDireccionInexistenteSeInformaComoSinDatos() {
        dadoUnaDireccionQueElMapaNoEncuentra();

        cuandoSeIntentaAnalizarElLugar("Calle inexistente 1");

        entoncesSeRechazaPorZonaSinDatos();
        entoncesNoSeAnalizaNingunaZona();
    }

    @Test
    @DisplayName("Falla con 404 si la dirección cae fuera de toda zona")
    void unaDireccionFueraDeTodaZonaSeInformaComoSinDatos() {
        dadoUnaDireccionQueCaeFueraDeTodaZona();

        cuandoSeIntentaAnalizarElLugar("Av. Corrientes 1234");

        entoncesSeRechazaPorZonaSinDatos();
    }

    @Test
    @DisplayName("Rechaza una dirección demasiado corta sin consultar el mapa")
    void rechazaUnaDireccionDemasiadoCortaSinConsultarElMapa() {
        dadoQueNoHayBarriosConocidos();

        cuandoSeIntentaAnalizarElLugar("ab");

        entoncesSeRechazaPorDireccionInvalida();
        entoncesNoSeConsultaElGeocodificador();
    }

    @Test
    @DisplayName("Devuelve la zona sin productos cuando aportan menos de 3 comercios")
    void devuelveLaZonaSinProductosCuandoAportanMenosDeTresComercios() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(2, unProductoDeZona("Yerba 1kg", 2, "12.50"));

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesSeInformaLaZonaDeDosComerciosSinProductos();
    }

    @Test
    @DisplayName("Devuelve los productos con 3 comercios, el mínimo")
    void devuelveLosProductosCuandoAportanTresComerciosQueEsElMinimo() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(3, unProductoDeZona("Yerba 1kg", 3, "12.50"));

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesSeInformaUnSoloProducto();
    }

    @Test
    @DisplayName("Ordena los productos por comercios que venden y luego por unidades por semana")
    void ordenaLosProductosPorComerciosQueVendenYLuegoPorUnidadesPorSemana() {
        dadoUnPuntoDentroDeLaZonaConTresProductos();

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesLosProductosVanEnEsteOrden("Yerba", "Fideos", "Azúcar");
    }

    @Test
    @DisplayName("Con el cupo del plan gratuito deja ver primero los productos más vendidos")
    void conElCupoDelPlanGratuitoDejaVerPrimeroLosProductosMasVendidos() {
        dadoUnPuntoConDosProductosYUnPlanQueAutorizaSoloElPrimero();

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesLosProductosVanEnEsteOrden("Yerba");
    }

    @Test
    @DisplayName("Gasta el cupo propio de Otra zona, aparte del de las pestañas de Mi zona")
    void gastaElCupoPropioDeOtraZonaAparteDelDeLasPestanasDeMiZona() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(10, unProductoDeZona("Yerba", 9, "5"));

        cuandoSeAnalizaElPuntoDelMapa();

        entoncesSeGastaElCupoDeOtraZona();
    }

    // --- dado ---
    private void dadoUnPuntoDentroDeLaZonaConUnaYerba() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(10, unProductoDeZona("Yerba 1kg", 6, "12.50"));
    }

    private void dadoUnPuntoDentroDeLaZonaConTresProductos() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(10, unProductoDeZona("Azúcar", 4, "20"),
                unProductoDeZona("Yerba", 9, "5"), unProductoDeZona("Fideos", 4, "30"));
    }

    private void dadoUnPuntoConDosProductosYUnPlanQueAutorizaSoloElPrimero() {
        dadoUnPuntoDentroDeLaZonaConAnalisis(10, unProductoDeZona("Azúcar", 4, "20"),
                unProductoDeZona("Yerba", 9, "5"));
        controlPlanService = mock(ControlPlanService.class, autorizaSoloLaPrimera());
    }

    private void dadoUnPuntoDentroDeLaZonaConAnalisis(long comercios, AgregadoZonaProducto... productos) {
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        dadoElAnalisisDeLaZona(comercios, productos);
    }

    private void dadoUnPuntoFueraDeTodaZona() {
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());
    }

    private void dadoQueElBarrioEsConocidoYTieneUnaYerba() {
        when(zonaRepository.findAll()).thenReturn(List.of(unaZonaDeVillaCrespo(), zona));
        dadoElAnalisisDeLaZona(10, unProductoDeZona("Yerba 1kg", 6, "12.50"));
    }

    private void dadoUnaDireccionQueCaeDentroDeLaZona() {
        dadoUnaDireccionEncontrada(DIRECCION);
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        dadoElAnalisisDeLaZona(10, unProductoDeZona("Yerba 1kg", 6, "12.50"));
    }

    private void dadoUnaDireccionQueElMapaNoEncuentra() {
        when(zonaRepository.findAll()).thenReturn(List.of(unaZonaDeVillaCrespo()));
        when(geocodificadorPort.buscar("Calle inexistente 1", AHORA)).thenReturn(List.of());
    }

    private void dadoUnaDireccionQueCaeFueraDeTodaZona() {
        dadoUnaDireccionEncontrada("Av. Corrientes 1234");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());
    }

    private void dadoQueNoHayBarriosConocidos() {
        when(zonaRepository.findAll()).thenReturn(List.of());
    }

    private void dadoUnaDireccionEncontrada(String lugar) {
        when(zonaRepository.findAll()).thenReturn(List.of(unaZonaDeVillaCrespo()));
        when(geocodificadorPort.buscar(lugar, AHORA))
                .thenReturn(List.of(new DireccionEncontrada(lugar, LATITUD, LONGITUD, "San Justo")));
    }

    private void dadoElAnalisisDeLaZona(long comercios, AgregadoZonaProducto... productos) {
        when(agregadoZonaRepository.analizar(zona)).thenReturn(unAnalisisDeZona(comercios, productos));
    }

    // --- cuando ---
    private void cuandoSeAnalizaElPuntoDelMapa() {
        analizar(pedidoConPunto(null));
    }

    private void cuandoSeAnalizaElPuntoDelMapaConElLugar(String lugar) {
        analizar(pedidoConPunto(lugar));
    }

    private void cuandoSeAnalizaElLugar(String lugar) {
        analizar(pedidoConLugar(lugar));
    }

    private void cuandoSeIntentaAnalizarElPuntoDelMapa() {
        rechazo = catchThrowable(() -> analizar(pedidoConPunto(null)));
    }

    private void cuandoSeIntentaAnalizarElLugar(String lugar) {
        rechazo = catchThrowable(() -> analizar(pedidoConLugar(lugar)));
    }

    private void analizar(AnalizarZonaRequest pedido) {
        zonaItem = casoDeUso().execute(pedido);
    }

    private AnalizarZona casoDeUso() {
        return new ZonaUseCaseConfig().analizarZona(zonaRepository, geocodificadorPort, agregadoZonaRepository,
                CLOCK, negocioRepository, controlPlanService);
    }

    private AnalizarZonaRequest pedidoConPunto(String lugar) {
        return new AnalizarZonaRequest(lugar, LATITUD, LONGITUD, USUARIO_ID);
    }

    private AnalizarZonaRequest pedidoConLugar(String lugar) {
        return new AnalizarZonaRequest(lugar, null, null, USUARIO_ID);
    }

    // --- entonces ---
    private void entoncesSeInformaLaZonaConLosDatosDeLaYerba() {
        assertThat(zonaItem.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(zonaItem.comercios()).isEqualTo(10);
        assertThat(zonaItem.productos()).singleElement().satisfies(producto -> {
            assertThat(producto.nombre()).isEqualTo("Yerba 1kg");
            assertThat(producto.comercios()).isEqualTo(8);
            assertThat(producto.precioMinimo()).isEqualByComparingTo("2800");
            assertThat(producto.precioPromedio()).isEqualByComparingTo("3100");
            assertThat(producto.precioMaximo()).isEqualByComparingTo("3400");
            assertThat(producto.comerciosVenden()).isEqualTo(6);
            assertThat(producto.unidadesPorSemana()).isEqualByComparingTo("12.5");
        });
    }

    private void entoncesSeInformaLaZonaDeSanJusto() {
        assertThat(zonaItem.nombre()).isEqualTo(NOMBRE_ZONA);
    }

    private void entoncesSeInformanDiezComercios() {
        assertThat(zonaItem.comercios()).isEqualTo(10);
    }

    private void entoncesSeInformaLaZonaDeDosComerciosSinProductos() {
        assertThat(zonaItem.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(zonaItem.comercios()).isEqualTo(2);
        assertThat(zonaItem.productos()).isEmpty();
    }

    private void entoncesSeInformaUnSoloProducto() {
        assertThat(zonaItem.productos()).hasSize(1);
    }

    private void entoncesLosProductosVanEnEsteOrden(String... nombres) {
        assertThat(zonaItem.productos()).extracting(ProductoItem::nombre).containsExactly(nombres);
    }

    private void entoncesSeResuelveLaZonaDelPunto() {
        verify(zonaRepository).resolver(LATITUD, LONGITUD);
    }

    private void entoncesNoSeConsultaElGeocodificador() {
        verifyNoInteractions(geocodificadorPort);
    }

    private void entoncesNoSeAnalizaNingunaZona() {
        verifyNoInteractions(agregadoZonaRepository);
    }

    private void entoncesSeRechazaPorZonaSinDatos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage(MENSAJE_SIN_DATOS);
    }

    private void entoncesSeRechazaPorDireccionInvalida() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeGastaElCupoDeOtraZona() {
        verify(controlPlanService).seleccionar(any(), eq(TipoContenidoPlan.ZONA_OTRA), any(), any());
    }
}
