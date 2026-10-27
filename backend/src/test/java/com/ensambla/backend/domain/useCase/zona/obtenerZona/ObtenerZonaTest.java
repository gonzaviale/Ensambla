package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.zona.ZonaItem;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerZonaTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class,
            invocation -> autorizaTodo(invocation.getArgument(2)));
    private final ObtenerZona obtenerZona = new ZonaUseCaseConfig().obtenerZona(
            negocioRepository, zonaRepository, agregadoZonaRepository, controlPlanService, CLOCK);

    @Test
    @DisplayName("Devuelve los agregados de la zona del negocio de la sesión")
    void executeShouldReturnZoneOfNegocio() {
        Zona zona = givenZona();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().zonaId(ZONA_ID).build());
        when(zonaRepository.findByIdSiTiene(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(givenAnalisis());

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
    @DisplayName("Devuelve una zona vacía cuando el negocio todavía no tiene zona")
    void executeShouldReturnEmptyZoneWhenNegocioHasNone() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(Negocio.builder().build());
        when(zonaRepository.findByIdSiTiene(null)).thenReturn(Optional.empty());

        ZonaItem result = obtenerZona.execute(new ObtenerZonaRequest(USUARIO_ID));

        assertThat(result.nombre()).isEmpty();
        assertThat(result.comercios()).isZero();
        assertThat(result.productos()).isEmpty();
        verifyNoInteractions(agregadoZonaRepository);
    }

    // --- arrange ---
    private static AccesoContenidoItem autorizaTodo(List<String> claves) {
        return new AccesoContenidoItem(claves, 0, null, claves.size(), 0, null);
    }

    private Zona givenZona() {
        return Zona.builder().id(ZONA_ID).nombre("San Justo - UNLaM").localidad("San Justo").build();
    }

    private AnalisisZona givenAnalisis() {
        return new AnalisisZona("San Justo - UNLaM", 10, List.of(new AgregadoZonaProducto("Yerba 1kg", 8, 6,
                new BigDecimal("2800.00"), new BigDecimal("3100.00"), new BigDecimal("3400.00"),
                new BigDecimal("12.50"))));
    }
}
