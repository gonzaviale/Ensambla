package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZonaRequest;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZonaRequest;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ZonaUseCaseTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ZonaService zonaService = mock(ZonaService.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final UbicacionService ubicacionService = mock(UbicacionService.class);
    private final AgregadoZonaService agregadoZonaService = mock(AgregadoZonaService.class);
    private final AccesoZonaService accesoZonaService = mock(AccesoZonaService.class,
            invocation -> invocation.getArgument(1));
    private final ZonaUseCaseConfig config = new ZonaUseCaseConfig();
    private final ObtenerZona obtenerZona = config.obtenerZona(
            negocioService, zonaService, agregadoZonaService, accesoZonaService, CLOCK);
    private final AnalizarZona analizarZona = config.analizarZona(
            zonaService, zonaRepository, ubicacionService, agregadoZonaService, CLOCK, negocioService, accesoZonaService);

    @Test
    @DisplayName("Obtener devuelve los agregados de la zona del negocio de la sesión")
    void obtenerShouldReturnZoneOfNegocio() {
        Zona zona = givenZona();
        when(negocioService.obtenerConfiguracion(USUARIO_ID))
                .thenReturn(Negocio.builder().zonaId(ZONA_ID).build());
        when(zonaService.obtener(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaService.analizar(zona)).thenReturn(givenAnalisis());

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID));

        assertThat(result.nombre()).isEqualTo("San Justo - UNLaM");
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
    }

    @Test
    @DisplayName("Obtener devuelve una zona vacía cuando el negocio todavía no tiene zona")
    void obtenerShouldReturnEmptyZoneWhenNegocioHasNone() {
        when(negocioService.obtenerConfiguracion(USUARIO_ID)).thenReturn(Negocio.builder().build());
        when(zonaService.obtener(null)).thenReturn(Optional.empty());

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID));

        assertThat(result).isEqualTo(new ZonaItem("", 0, List.of()));
        verifyNoInteractions(agregadoZonaService);
    }

    @Test
    @DisplayName("Analizar encuentra el primer barrio cuyo nombre o localidad coincide, sin consultar el mapa")
    void analizarShouldFindBarrioByName() {
        Zona zona = givenZona();
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona(), zona));
        when(agregadoZonaService.analizar(zona)).thenReturn(givenAnalisis());

        ZonaItem result = analizarZona.execute(new AnalizarZonaRequest("san justo", USUARIO_ID));

        assertThat(result.nombre()).isEqualTo("San Justo - UNLaM");
        verifyNoInteractions(ubicacionService);
    }

    @Test
    @DisplayName("Analizar resuelve la zona de una dirección cuando no es un barrio conocido")
    void analizarShouldResolveZoneFromAddress() {
        Zona zona = givenZona();
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        when(ubicacionService.buscar("Arieta 3120, San Justo", AHORA)).thenReturn(
                List.of(new DireccionEncontrada("Arieta 3120", LATITUD, LONGITUD, "San Justo")));
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.of(zona));
        when(agregadoZonaService.analizar(zona)).thenReturn(givenAnalisis());

        ZonaItem result = analizarZona.execute(new AnalizarZonaRequest("Arieta 3120, San Justo", USUARIO_ID));

        assertThat(result.comercios()).isEqualTo(10);
    }

    @Test
    @DisplayName("Analizar falla con 404 si la dirección no se encuentra")
    void analizarShouldFailWhenAddressNotFound() {
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        when(ubicacionService.buscar("Calle inexistente 1", AHORA)).thenReturn(List.of());

        assertThatThrownBy(() -> analizarZona.execute(new AnalizarZonaRequest("Calle inexistente 1", USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Todavía no tenemos datos de esa zona");
        verifyNoInteractions(agregadoZonaService);
    }

    @Test
    @DisplayName("Analizar falla con 404 si la dirección cae fuera de toda zona")
    void analizarShouldFailWhenAddressOutsideEveryZone() {
        when(zonaRepository.findAll()).thenReturn(List.of(otraZona()));
        when(ubicacionService.buscar("Av. Corrientes 1234", AHORA)).thenReturn(
                List.of(new DireccionEncontrada("Av. Corrientes 1234", LATITUD, LONGITUD, "CABA")));
        when(zonaService.resolver(LATITUD, LONGITUD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analizarZona.execute(new AnalizarZonaRequest("Av. Corrientes 1234", USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Analizar deja pasar el error del buscador de direcciones")
    void analizarShouldPropagateAddressSearchError() {
        when(zonaRepository.findAll()).thenReturn(List.of());
        when(ubicacionService.buscar("ab", AHORA)).thenThrow(new BusinessRuleException("Escribí una dirección"));

        assertThatThrownBy(() -> analizarZona.execute(new AnalizarZonaRequest("ab", USUARIO_ID)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").localidad("San Justo").build();
    }

    private static Zona otraZona() {
        return Zona.builder().id(ZonaId.nuevo()).nombre("Villa Crespo").localidad("CABA").build();
    }

    private AnalisisZona givenAnalisis() {
        return new AnalisisZona("San Justo - UNLaM", 10, List.of(new AgregadoZonaProducto("Yerba 1kg", 8, 6,
                new BigDecimal("2800.00"), new BigDecimal("3100.00"), new BigDecimal("3400.00"),
                new BigDecimal("12.50"))));
    }
}
