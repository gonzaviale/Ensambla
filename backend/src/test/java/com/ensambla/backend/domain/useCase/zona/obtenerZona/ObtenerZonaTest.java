package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.VistaZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.zona.ProductosPropiosService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerZonaTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final ProductosPropiosService productosPropiosService = mock(ProductosPropiosService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class, autorizaTodo());
    private final ObtenerZona obtenerZona = obtenerZona(controlPlanService);

    @Test
    @DisplayName("Devuelve los agregados de la zona del negocio de la sesión")
    void executeShouldReturnZoneOfNegocio() {
        givenNegocioConZona();
        when(agregadoZonaRepository.analizar(any())).thenReturn(givenAnalisis(givenProducto("Yerba 1kg")));

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.PRECIOS));

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
    @DisplayName("Devuelve una zona vacía cuando el negocio todavía no tiene zona, sin mirar su inventario")
    void executeShouldReturnEmptyZoneWhenNegocioHasNone() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(Negocio.builder().build());
        when(zonaRepository.findByIdSiTiene(null)).thenReturn(Optional.empty());

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.PRECIOS));

        assertThat(result.nombre()).isEmpty();
        assertThat(result.comercios()).isZero();
        assertThat(result.productos()).isEmpty();
        verifyNoInteractions(agregadoZonaRepository, productosPropiosService);
    }

    @Test
    @DisplayName("Precios gasta su cupo y deja ver primero los productos que el negocio tiene")
    void executeShouldOfferOwnedProductsFirstInPrices() {
        ControlPlanService soloLaPrimera = mock(ControlPlanService.class, autorizaSoloLaPrimera());
        givenNegocioConZona();
        givenInventario("Yerba");
        when(agregadoZonaRepository.analizar(any())).thenReturn(
                givenAnalisis(givenProducto("Azúcar 1 kg"), givenProducto("Yerba 1kg")));

        ZonaItem result = obtenerZona(soloLaPrimera).execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.PRECIOS));

        assertThat(result.productos()).extracting(ProductoItem::nombre).containsExactly("Yerba 1kg");
        verify(soloLaPrimera).seleccionar(eq(NEGOCIO_ID), eq(TipoContenidoPlan.ZONA), any(), any());
    }

    @Test
    @DisplayName("Qué se vende cerca gasta su propio cupo y deja ver primero lo que el negocio todavía no tiene")
    void executeShouldOfferMissingProductsFirstInNearby() {
        ControlPlanService soloLaPrimera = mock(ControlPlanService.class, autorizaSoloLaPrimera());
        givenNegocioConZona();
        givenInventario("Yerba");
        when(agregadoZonaRepository.analizar(any())).thenReturn(
                givenAnalisis(givenProducto("Yerba 1kg"), givenProducto("Azúcar 1 kg")));

        ZonaItem result = obtenerZona(soloLaPrimera).execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.CERCA));

        assertThat(result.productos()).extracting(ProductoItem::nombre).containsExactly("Azúcar 1 kg");
        verify(soloLaPrimera).seleccionar(eq(NEGOCIO_ID), eq(TipoContenidoPlan.ZONA_CERCA), any(), any());
    }

    @Test
    @DisplayName("Una vista que no ordena por el inventario no lo consulta y gasta su propio cupo")
    void executeShouldNotLookAtInventoryWhenTheViewDoesNotUseIt() {
        givenNegocioConZona();
        when(agregadoZonaRepository.analizar(any())).thenReturn(
                givenAnalisis(givenProducto("Yerba 1kg"), givenProducto("Azúcar 1 kg")));

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID, VistaZona.OTRA));

        assertThat(result.productos()).hasSize(2);
        verifyNoInteractions(productosPropiosService);
        verify(controlPlanService).seleccionar(eq(NEGOCIO_ID), eq(TipoContenidoPlan.ZONA_OTRA), any(), any());
    }

    // --- arrange ---
    private void givenNegocioConZona() {
        Zona zona = Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").localidad("San Justo").build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).zonaId(ZONA_ID).build());
        when(zonaRepository.findByIdSiTiene(ZONA_ID)).thenReturn(Optional.of(zona));
    }

    private void givenInventario(String... nombres) {
        when(productosPropiosService.nombresDelInventario(NEGOCIO_ID)).thenReturn(List.of(nombres));
    }

    private static AnalisisZona givenAnalisis(AgregadoZonaProducto... productos) {
        return new AnalisisZona("San Justo - UNLaM", 10, List.of(productos));
    }

    private static AgregadoZonaProducto givenProducto(String nombre) {
        return new AgregadoZonaProducto(nombre, 8, 6, new BigDecimal("2800.00"), new BigDecimal("3100.00"),
                new BigDecimal("3400.00"), new BigDecimal("12.50"));
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

    // --- act ---
    private ObtenerZona obtenerZona(ControlPlanService plan) {
        return new ZonaUseCaseConfig().obtenerZona(negocioRepository, zonaRepository, agregadoZonaRepository, plan,
                productosPropiosService, CLOCK);
    }
}
