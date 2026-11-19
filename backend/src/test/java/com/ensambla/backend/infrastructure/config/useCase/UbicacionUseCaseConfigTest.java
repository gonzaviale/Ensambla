package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.port.GeocodificadorPort;
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
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final String DIRECCION = "Florencio Varela 1903, San Justo";
    private static final BigDecimal LATITUD = new BigDecimal("-34.6701");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.5625");

    private final UbicacionUseCaseConfig config = new UbicacionUseCaseConfig();
    private final GeocodificadorPort geocodificador = mock(GeocodificadorPort.class);
    private BuscarDireccionesResponse direcciones;
    private ResolverLocalidadResponse localidad;
    private ListarRubrosResponse rubros;

    @Test
    @DisplayName("La búsqueda usa el reloj del caso de uso y mapea las coordenadas sin perder precisión")
    void laBusquedaUsaElRelojYMapeaLasCoordenadas() {
        dadoQueElGeocodificadorEncuentraLaDireccion();

        cuandoSeBuscaLaDireccion();

        entoncesSeEncontroLaDireccionEn("San Justo");
    }

    @Test
    @DisplayName("Resolver la localidad usa el reloj del caso de uso y devuelve la principal y todas las válidas")
    void resolverLaLocalidadDevuelveLaPrincipalYLasValidas() {
        dadoQueElPuntoEstaEn("San Justo", "Villa Luzuriaga");

        cuandoSeResuelveLaLocalidad();

        entoncesLaLocalidadEs("San Justo", "San Justo", "Villa Luzuriaga");
    }

    @Test
    @DisplayName("Sin localidades reconocidas la respuesta no inventa una principal")
    void sinLocalidadesLaRespuestaNoInventaUnaPrincipal() {
        cuandoSeRespondeSinLocalidades();

        entoncesNoHayLocalidad();
    }

    @Test
    @DisplayName("El listado conecta el catálogo de dominio con la respuesta de rubros")
    void elListadoConectaElCatalogoDeRubros() {
        cuandoSeListanLosRubros();

        entoncesLosRubrosSon(List.of("ALMACEN", "KIOSCO", "MINIMERCADO", "DIETETICA", "PAPELERIA", "OTRO"),
                List.of("Almacén", "Kiosco", "Minimercado", "Dietética", "Papelería", "Otro"));
    }

    @Test
    @DisplayName("Una búsqueda sin coincidencias conserva la respuesta vacía")
    void unaBusquedaSinCoincidenciasQuedaVacia() {
        entoncesNoHayResultados(BuscarDireccionesResponse.de(List.of()));
    }

    @Test
    @DisplayName("La respuesta de rubros admite un catálogo vacío")
    void laRespuestaDeRubrosAdmiteUnCatalogoVacio() {
        entoncesNoHayRubros(ListarRubrosResponse.de(List.of()));
    }

    private void dadoQueElGeocodificadorEncuentraLaDireccion() {
        when(geocodificador.buscar(DIRECCION, AHORA))
                .thenReturn(List.of(new DireccionEncontrada(DIRECCION, LATITUD, LONGITUD, "San Justo")));
    }

    private void dadoQueElPuntoEstaEn(String... localidades) {
        when(geocodificador.resolverLocalidades(LATITUD, LONGITUD, AHORA))
                .thenReturn(new LocalidadesDelPunto(List.of(localidades)));
    }

    private void cuandoSeBuscaLaDireccion() {
        direcciones = config.buscarDirecciones(geocodificador, RELOJ).execute(new BuscarDireccionesRequest(DIRECCION));
    }

    private void cuandoSeResuelveLaLocalidad() {
        localidad = config.resolverLocalidad(geocodificador, RELOJ).execute(new ResolverLocalidadRequest(LATITUD, LONGITUD));
    }

    private void cuandoSeRespondeSinLocalidades() {
        localidad = ResolverLocalidadResponse.de(new LocalidadesDelPunto(List.of()));
    }

    private void cuandoSeListanLosRubros() {
        rubros = config.listarRubros().execute();
    }

    private void entoncesSeEncontroLaDireccionEn(String enLocalidad) {
        assertThat(direcciones.resultados()).singleElement().satisfies(resultado -> {
            assertThat(resultado.direccion()).isEqualTo(DIRECCION);
            assertThat(resultado.latitud()).isEqualTo(LATITUD);
            assertThat(resultado.longitud()).isEqualTo(LONGITUD);
            assertThat(resultado.localidad()).isEqualTo(enLocalidad);
        });
    }

    private void entoncesLaLocalidadEs(String principal, String... todas) {
        assertThat(localidad.localidad()).isEqualTo(principal);
        assertThat(localidad.localidades()).containsExactly(todas);
    }

    private void entoncesNoHayLocalidad() {
        assertThat(localidad.localidad()).isNull();
        assertThat(localidad.localidades()).isEmpty();
    }

    private void entoncesLosRubrosSon(List<String> codigos, List<String> nombres) {
        assertThat(rubros.rubros()).extracting(ListarRubrosResponse.RubroItem::codigo).containsExactlyElementsOf(codigos);
        assertThat(rubros.rubros()).extracting(ListarRubrosResponse.RubroItem::nombre).containsExactlyElementsOf(nombres);
    }

    private static void entoncesNoHayResultados(BuscarDireccionesResponse respuesta) {
        assertThat(respuesta.resultados()).isEmpty();
    }

    private static void entoncesNoHayRubros(ListarRubrosResponse respuesta) {
        assertThat(respuesta.rubros()).isEmpty();
    }
}