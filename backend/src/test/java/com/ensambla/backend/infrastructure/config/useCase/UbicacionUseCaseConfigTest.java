package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesRequest;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesResponse;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubrosResponse;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidadRequest;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidadResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UbicacionUseCaseConfigTest {

    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final String DIRECCION = "Florencio Varela 1903, San Justo";
    private static final BigDecimal LATITUD = new BigDecimal("-34.6701");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.5625");
    private final UbicacionUseCaseConfig config = new UbicacionUseCaseConfig();

    @Test
    @DisplayName("La búsqueda usa el reloj del caso de uso y mapea las coordenadas sin perder precisión")
    void buscarDireccionesUseCaseShouldWireClockAndMapper() {
        UbicacionService ubicacionService = mock(UbicacionService.class);
        when(ubicacionService.buscar(DIRECCION, AHORA))
                .thenReturn(List.of(new DireccionEncontrada(DIRECCION, LATITUD, LONGITUD, "San Justo")));

        var response = config.buscarDirecciones(ubicacionService,
                Clock.fixed(AHORA, ZoneOffset.UTC)).execute(new BuscarDireccionesRequest(DIRECCION));

        assertThat(response.resultados()).hasSize(1);
        assertThat(response.resultados().getFirst().direccion()).isEqualTo(DIRECCION);
        assertThat(response.resultados().getFirst().latitud()).isEqualTo(LATITUD);
        assertThat(response.resultados().getFirst().longitud()).isEqualTo(LONGITUD);
        assertThat(response.resultados().getFirst().localidad()).isEqualTo("San Justo");
    }

    @Test
    @DisplayName("Resolver la localidad usa el reloj del caso de uso y devuelve la principal y todas las válidas")
    void resolverLocalidadUseCaseShouldWireClockAndMapper() {
        UbicacionService ubicacionService = mock(UbicacionService.class);
        when(ubicacionService.resolverLocalidades(LATITUD, LONGITUD, AHORA))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo", "Villa Luzuriaga")));

        var response = config.resolverLocalidad(ubicacionService,
                Clock.fixed(AHORA, ZoneOffset.UTC)).execute(new ResolverLocalidadRequest(LATITUD, LONGITUD));

        assertThat(response.localidad()).isEqualTo("San Justo");
        assertThat(response.localidades()).containsExactly("San Justo", "Villa Luzuriaga");
    }

    @Test
    @DisplayName("Sin localidades reconocidas la respuesta no inventa una principal")
    void resolverLocalidadShouldPreserveEmptyLocalities() {
        var response = ResolverLocalidadResponse.de(new LocalidadesDelPunto(List.of()));

        assertThat(response.localidad()).isNull();
        assertThat(response.localidades()).isEmpty();
    }

    @Test
    @DisplayName("El listado conecta el catálogo de dominio con la respuesta de rubros")
    void listarRubrosUseCaseShouldWireCatalogAndMapper() {
        var useCase = config.listarRubros();

        var response = useCase.execute();

        assertThat(response.rubros()).extracting(ListarRubrosResponse.RubroItem::codigo)
                .containsExactly("ALMACEN", "KIOSCO", "MINIMERCADO", "DIETETICA", "OTRO");
        assertThat(response.rubros()).extracting(ListarRubrosResponse.RubroItem::nombre)
                .containsExactly("Almacén", "Kiosco", "Minimercado", "Dietética", "Otro");
        assertThat(response.rubros().getFirst().codigo()).isEqualTo("ALMACEN");
        assertThat(response.rubros().getFirst().nombre()).isEqualTo("Almacén");
    }

    @Test
    @DisplayName("Una búsqueda sin coincidencias conserva la respuesta vacía")
    void buscarDireccionesShouldPreserveEmptyResults() {
        var response = BuscarDireccionesResponse.de(List.of());

        assertThat(response.resultados()).isEmpty();
    }

    @Test
    @DisplayName("La respuesta de rubros admite un catálogo vacío")
    void listarRubrosShouldPreserveEmptyCatalog() {
        var response = ListarRubrosResponse.de(List.of());

        assertThat(response.rubros()).isEmpty();
    }
}
