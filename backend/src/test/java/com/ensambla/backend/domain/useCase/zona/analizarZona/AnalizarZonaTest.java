package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalizarZonaTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");
    private static final String NOMBRE_ZONA = "San Justo - UNLaM";
    private static final String MENSAJE_SIN_DATOS = "Todavía no tenemos datos de esa zona";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final GeocodificadorPort geocodificadorPort = mock(GeocodificadorPort.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);

    @Test
    @DisplayName("Resuelve la zona del punto del mapa sin buscar el texto ni la dirección")
    void executeShouldResolveZoneByPoint() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(conPunto(null));

        assertThat(result.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(result.comercios()).isEqualTo(10);
        assertThat(result.productos()).singleElement().satisfies(producto -> {
            assertThat(producto.nombre()).isEqualTo("Yerba 1kg");
            assertThat(producto.comercios()).isEqualTo(8);
            assertThat(producto.precioMinimo()).isEqualByComparingTo("2800");
            assertThat(producto.precioPromedio()).isEqualByComparingTo("3100");
            assertThat(producto.precioMaximo()).isEqualByComparingTo("3400");
            assertThat(producto.comerciosVenden()).isEqualTo(6);
            assertThat(producto.unidadesPorSemana()).isEqualByComparingTo("12.5");
        });
        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Con punto y lugar a la vez manda el punto")
    void executeShouldPreferPointOverPlace() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        execute(conPunto("Palermo"));

        verify(zonaRepository).resolver(LATITUD, LONGITUD);
        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Falla con 404 si el punto cae fuera de toda zona")
    void executeShouldFailWhenPointIsOutsideEveryZone() {
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(conPunto(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MENSAJE_SIN_DATOS);
        verifyNoInteractions(agregadoZonaRepository);
    }

    @Test
    @DisplayName("Encuentra el primer barrio cuyo nombre o localidad coincide, sin consultar el mapa")
    void executeShouldFindBarrioByName() {
        Zona zona = givenZona();
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona(), zona));
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(conLugar("san justo"));

        assertThat(result.nombre()).isEqualTo(NOMBRE_ZONA);
        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Resuelve la zona de una dirección cuando no es un barrio conocido")
    void executeShouldResolveZoneFromAddress() {
        Zona zona = givenZona();
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        givenDireccion("Arieta 3120, San Justo");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(conLugar("Arieta 3120, San Justo"));

        assertThat(result.comercios()).isEqualTo(10);
    }

    @Test
    @DisplayName("Falla con 404 si la dirección no se encuentra")
    void executeShouldFailWhenAddressNotFound() {
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        when(geocodificadorPort.buscar("Calle inexistente 1", AHORA)).thenReturn(List.of());

        assertThatThrownBy(() -> execute(conLugar("Calle inexistente 1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MENSAJE_SIN_DATOS);
        verifyNoInteractions(agregadoZonaRepository);
    }

    @Test
    @DisplayName("Falla con 404 si la dirección cae fuera de toda zona")
    void executeShouldFailWhenAddressOutsideEveryZone() {
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        givenDireccion("Av. Corrientes 1234");
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(conLugar("Av. Corrientes 1234")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza una dirección demasiado corta sin consultar el mapa")
    void executeShouldRejectShortAddress() {
        when(zonaRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> execute(conLugar("ab")))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(geocodificadorPort);
    }

    @Test
    @DisplayName("Devuelve la zona sin productos cuando aportan menos de 3 comercios")
    void executeShouldReturnNoProductsWhenFewerThanThreeCommerces() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 2, givenProducto("Yerba 1kg", 2, "12.50"));

        ZonaItem result = execute(conPunto(null));

        assertThat(result.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(result.comercios()).isEqualTo(2);
        assertThat(result.productos()).isEmpty();
    }

    @Test
    @DisplayName("Devuelve los productos con 3 comercios, el mínimo")
    void executeShouldReturnProductsWithExactlyThreeCommerces() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 3, givenProducto("Yerba 1kg", 3, "12.50"));

        ZonaItem result = execute(conPunto(null));

        assertThat(result.productos()).hasSize(1);
    }

    @Test
    @DisplayName("Ordena los productos por comercios que venden y luego por unidades por semana")
    void executeShouldRankProductsBySales() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10,
                givenProducto("Azúcar", 4, "20"),
                givenProducto("Yerba", 9, "5"),
                givenProducto("Fideos", 4, "30"));

        ZonaItem result = execute(conPunto(null));

        assertThat(result.productos()).extracting(ProductoItem::nombre)
                .containsExactly("Yerba", "Fideos", "Azúcar");
    }

    @Test
    @DisplayName("Con el cupo del plan gratuito deja ver primero los productos más vendidos")
    void executeShouldOfferBestSellersFirstToThePlanQuota() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10,
                givenProducto("Azúcar", 4, "20"),
                givenProducto("Yerba", 9, "5"));

        ZonaItem result = analizarZona(autorizaSoloLaPrimera()).execute(conPunto(null));

        assertThat(result.productos()).extracting(ProductoItem::nombre).containsExactly("Yerba");
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(ZONA_ID).nombre(NOMBRE_ZONA).localidad("San Justo").build();
    }

    private static Zona otraZona() {
        return Zona.builder().id(ZonaId.nuevo()).nombre("Villa Crespo").localidad("CABA").build();
    }

    private Zona givenZonaResolvedByPoint() {
        Zona zona = givenZona();
        when(zonaRepository.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        return zona;
    }

    private void givenDireccion(String lugar) {
        when(geocodificadorPort.buscar(lugar, AHORA))
                .thenReturn(List.of(new DireccionEncontrada(lugar, LATITUD, LONGITUD, "San Justo")));
    }

    private void givenAnalisis(Zona zona, long comercios, AgregadoZonaProducto... productos) {
        when(agregadoZonaRepository.analizar(zona))
                .thenReturn(new AnalisisZona(NOMBRE_ZONA, comercios, List.of(productos)));
    }

    private static Answer<AccesoContenidoItem> autorizaTodo() {
        return invocation -> {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves, 0, null, claves.size(), 0, null);
        };
    }

    private static Answer<AccesoContenidoItem> autorizaSoloLaPrimera() {
        return invocation -> {
            List<String> claves = invocation.getArgument(2);
            return new AccesoContenidoItem(claves.subList(0, 1), 1, 1, claves.size(), claves.size() - 1, null);
        };
    }

    // --- helpers ---
    private static AnalizarZonaRequest conPunto(String lugar) {
        return new AnalizarZonaRequest(lugar, LATITUD, LONGITUD, USUARIO_ID);
    }

    private static AnalizarZonaRequest conLugar(String lugar) {
        return new AnalizarZonaRequest(lugar, null, null, USUARIO_ID);
    }

    private AgregadoZonaProducto givenProducto(String nombre, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("2800.00"),
                new BigDecimal("3100.00"), new BigDecimal("3400.00"), new BigDecimal(unidadesSemana));
    }

    // --- act ---
    private ZonaItem execute(AnalizarZonaRequest request) {
        return analizarZona(autorizaTodo()).execute(request);
    }

    private AnalizarZona analizarZona(Answer<AccesoContenidoItem> plan) {
        ControlPlanService controlPlanService = mock(ControlPlanService.class, plan);
        return new ZonaUseCaseConfig().analizarZona(
                zonaRepository, geocodificadorPort, agregadoZonaRepository, CLOCK, negocioRepository,
                controlPlanService);
    }
}
