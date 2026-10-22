package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.dto.zona.ObtenerZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import com.ensambla.backend.infrastructure.config.useCase.ZonaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ZonaUseCaseTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID ZONA_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ZonaService zonaService = mock(ZonaService.class);
    private final AgregadoZonaService agregadoZonaService = mock(AgregadoZonaService.class);
    private final ZonaUseCaseConfig config = new ZonaUseCaseConfig();
    private final ObtenerZonaUseCase obtenerZonaUseCase = config.obtenerZonaUseCase(
            negocioService, zonaService, agregadoZonaService, config.zonaItemMapper());

    @Test
    @DisplayName("Obtener devuelve los agregados de la zona del negocio de la sesión")
    void obtenerShouldReturnZoneOfNegocio() {
        Zona zona = givenZona();
        when(negocioService.obtenerConfiguracion(USUARIO_ID))
                .thenReturn(Negocio.builder().zonaId(ZONA_ID).build());
        when(zonaService.obtener(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaService.analizar(zona)).thenReturn(givenAnalisis());

        ZonaItem result = obtenerZonaUseCase.execute(new ObtenerZonaRequest(USUARIO_ID));

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

        ZonaItem result = obtenerZonaUseCase.execute(new ObtenerZonaRequest(USUARIO_ID));

        assertThat(result).isEqualTo(new ZonaItem("", 0, List.of()));
        verifyNoInteractions(agregadoZonaService);
    }

    // --- arrange ---
    private Zona givenZona() {
        return Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").localidad("San Justo").build();
    }

    private AnalisisZona givenAnalisis() {
        return new AnalisisZona("San Justo - UNLaM", 10, List.of(new AgregadoZonaProducto("Yerba 1kg", 8, 6,
                new BigDecimal("2800.00"), new BigDecimal("3100.00"), new BigDecimal("3400.00"),
                new BigDecimal("12.50"))));
    }
}
