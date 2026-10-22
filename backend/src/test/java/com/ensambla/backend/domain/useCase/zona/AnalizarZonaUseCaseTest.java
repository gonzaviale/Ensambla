package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.dto.zona.AnalizarZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.dto.zona.ZonaItem.ProductoItem;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalizarZonaUseCaseTest {

    private static final UUID ZONA_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");
    private static final String NOMBRE_ZONA = "San Justo - UNLaM";
    private static final String LUGAR = "Arieta 3120, San Justo";
    private static final String MENSAJE_SIN_DATOS = "Todavía no tenemos datos de esa zona";

    @Mock
    private ZonaService zonaService;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private AgregadoZonaService agregadoZonaService;

    private AnalizarZonaUseCase analizarZonaUseCase;

    @BeforeEach
    void setUp() {
        ZonaUseCaseConfig config = new ZonaUseCaseConfig();
        analizarZonaUseCase = config.analizarZonaUseCase(
                zonaService, ubicacionService, agregadoZonaService, config.zonaItemMapper(),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Resuelve la zona del punto del mapa sin buscar el texto ni la dirección")
    void executeShouldResolveZoneByPoint() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(new AnalizarZonaRequest(null, LATITUD, LONGITUD));

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
        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("Con punto y lugar a la vez manda el punto")
    void executeShouldPreferPointOverPlace() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        execute(new AnalizarZonaRequest("Palermo", LATITUD, LONGITUD));

        verify(zonaService).resolver(LATITUD, LONGITUD);
        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("Falla con 404 si el punto cae fuera de toda zona")
    void executeShouldFailWhenPointIsOutsideEveryZone() {
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(new AnalizarZonaRequest(null, LATITUD, LONGITUD)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MENSAJE_SIN_DATOS);
        verifyNoInteractions(agregadoZonaService);
    }

    @Test
    @DisplayName("Encuentra un barrio por su nombre sin consultar el mapa")
    void executeShouldFindBarrioByName() {
        Zona zona = givenZona();
        when(zonaService.buscarPorTexto("San Justo")).thenReturn(Optional.of(zona));
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(new AnalizarZonaRequest("San Justo", null, null));

        assertThat(result.nombre()).isEqualTo(NOMBRE_ZONA);
        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("Resuelve la zona de una dirección cuando no es un barrio conocido")
    void executeShouldResolveZoneFromAddress() {
        Zona zona = givenZona();
        when(zonaService.buscarPorTexto(LUGAR)).thenReturn(Optional.empty());
        when(ubicacionService.buscar(LUGAR, AHORA)).thenReturn(List.of(givenDireccion()));
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        givenAnalisis(zona, 10, givenProducto("Yerba 1kg", 6, "12.50"));

        ZonaItem result = execute(new AnalizarZonaRequest(LUGAR, null, null));

        assertThat(result.comercios()).isEqualTo(10);
    }

    @Test
    @DisplayName("Falla con 404 si la dirección no se encuentra")
    void executeShouldFailWhenAddressNotFound() {
        when(zonaService.buscarPorTexto(LUGAR)).thenReturn(Optional.empty());
        when(ubicacionService.buscar(LUGAR, AHORA)).thenReturn(List.of());

        assertThatThrownBy(() -> execute(new AnalizarZonaRequest(LUGAR, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MENSAJE_SIN_DATOS);
        verifyNoInteractions(agregadoZonaService);
    }

    @Test
    @DisplayName("Falla con 404 si la dirección cae fuera de toda zona")
    void executeShouldFailWhenAddressOutsideEveryZone() {
        when(zonaService.buscarPorTexto(LUGAR)).thenReturn(Optional.empty());
        when(ubicacionService.buscar(LUGAR, AHORA)).thenReturn(List.of(givenDireccion()));
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> execute(new AnalizarZonaRequest(LUGAR, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deja pasar el error del buscador de direcciones")
    void executeShouldPropagateAddressSearchError() {
        when(zonaService.buscarPorTexto("ab")).thenReturn(Optional.empty());
        when(ubicacionService.buscar("ab", AHORA)).thenThrow(new BusinessRuleException("Escribí una dirección"));

        assertThatThrownBy(() -> execute(new AnalizarZonaRequest("ab", null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Devuelve la zona sin productos cuando aportan menos de 3 comercios")
    void executeShouldReturnNoProductsWhenFewerThanThreeCommerces() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 2, givenProducto("Yerba 1kg", 2, "12.50"));

        ZonaItem result = execute(new AnalizarZonaRequest(null, LATITUD, LONGITUD));

        assertThat(result.nombre()).isEqualTo(NOMBRE_ZONA);
        assertThat(result.comercios()).isEqualTo(2);
        assertThat(result.productos()).isEmpty();
    }

    @Test
    @DisplayName("Devuelve los productos con 3 comercios, el mínimo")
    void executeShouldReturnProductsWithExactlyThreeCommerces() {
        Zona zona = givenZonaResolvedByPoint();
        givenAnalisis(zona, 3, givenProducto("Yerba 1kg", 3, "12.50"));

        ZonaItem result = execute(new AnalizarZonaRequest(null, LATITUD, LONGITUD));

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

        ZonaItem result = execute(new AnalizarZonaRequest(null, LATITUD, LONGITUD));

        assertThat(result.productos()).extracting(ProductoItem::nombre)
                .containsExactly("Yerba", "Fideos", "Azúcar");
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(ZONA_ID).nombre(NOMBRE_ZONA).localidad("San Justo").build();
    }

    private Zona givenZonaResolvedByPoint() {
        Zona zona = givenZona();
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        return zona;
    }

    private DireccionEncontrada givenDireccion() {
        return new DireccionEncontrada("Arieta 3120", LATITUD, LONGITUD, "San Justo");
    }

    private void givenAnalisis(Zona zona, long comercios, AgregadoZonaProducto... productos) {
        when(agregadoZonaService.analizar(zona))
                .thenReturn(new AnalisisZona(NOMBRE_ZONA, comercios, List.of(productos)));
    }

    private AgregadoZonaProducto givenProducto(String nombre, int comerciosVenden, String unidadesSemana) {
        return new AgregadoZonaProducto(nombre, 8, comerciosVenden, new BigDecimal("2800.00"),
                new BigDecimal("3100.00"), new BigDecimal("3400.00"), new BigDecimal(unidadesSemana));
    }

    // --- act ---
    private ZonaItem execute(AnalizarZonaRequest request) {
        return analizarZonaUseCase.execute(request);
    }
}
