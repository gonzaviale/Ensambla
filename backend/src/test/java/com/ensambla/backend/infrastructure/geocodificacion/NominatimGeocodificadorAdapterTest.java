package com.ensambla.backend.infrastructure.geocodificacion;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodificadorAdapterTest {

    private static final String URL = "https://geocodificador.test";
    private static final String CONSULTA = "Varela 1903, San Justo";
    private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");
    private static final String RESPUESTA = """
            [{"display_name":"Varela 1903, San Justo","lat":"-34.6701","lon":"-58.5625"}]
            """;

    private MockRestServiceServer servidor;
    private NominatimGeocodificadorAdapter adapter;
    private final AtomicLong tiempo = new AtomicLong();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL).defaultHeader("User-Agent", "Ensambla-test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new NominatimGeocodificadorAdapter(builder.build(), tiempo::get);
    }

    @AfterEach
    void tearDown() {
        servidor.verify();
    }

    @Test
    @DisplayName("Busca en Argentina con identificación, texto escapado y límite de cinco resultados")
    void buscarShouldSendExplicitIdentifiedSearch() {
        servidor.expect(requestTo(URL + "/search?q=Varela%201903%2C%20San%20Justo"
                        + "&format=jsonv2&limit=5&countrycodes=ar&accept-language=es"))
                .andExpect(header("User-Agent", "Ensambla-test"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        var resultados = buscar(CONSULTA, AHORA);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.getFirst().direccion()).isEqualTo(CONSULTA);
        assertThat(resultados.getFirst().latitud()).isEqualByComparingTo("-34.6701");
        assertThat(resultados.getFirst().longitud()).isEqualByComparingTo("-58.5625");
    }

    @Test
    @DisplayName("Reutiliza la caché aunque la consulta cambie de mayúsculas")
    void buscarShouldReuseCachedAddress() {
        givenResponse(RESPUESTA);

        var primero = buscar(CONSULTA, AHORA);
        var segundo = buscar(CONSULTA.toUpperCase(), AHORA.plusMillis(1));

        assertThat(segundo).isSameAs(primero);
    }

    @Test
    @DisplayName("Las búsquedas vacías también se cachean")
    void buscarShouldCacheEmptyResults() {
        givenResponse("[]");

        buscar(CONSULTA, AHORA);
        var resultados = buscar(CONSULTA, AHORA.plusSeconds(2));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("La caché vence a las 24 horas y renueva la entrada")
    void buscarShouldRefreshExpiredCache() {
        givenResponse(RESPUESTA);
        givenResponse("[]");

        buscar(CONSULTA, AHORA);
        var resultados = buscar(CONSULTA, AHORA.plusSeconds(86400));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("Dos consultas distintas seguidas no exceden la cuota global del proveedor")
    void buscarShouldRejectRequestsInsideGlobalInterval() {
        givenResponse(RESPUESTA);
        buscar(CONSULTA, AHORA);

        assertThatThrownBy(() -> buscar("Otra dirección", AHORA.plusMillis(1099)))
                .isInstanceOf(ServicioUbicacionException.class).hasMessageContaining("Esperá un segundo");
    }

    @Test
    @DisplayName("Una consulta nueva puede salir al terminar el intervalo mínimo")
    void buscarShouldAllowNextRequestAfterInterval() {
        givenResponse(RESPUESTA);
        givenResponse("[]");

        buscar(CONSULTA, AHORA);
        var resultados = buscar("Otra dirección", AHORA.plusMillis(1100));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("El límite usa el momento de salida aunque los pedidos traigan instantes anteriores")
    void buscarShouldRateLimitAtActualDispatchTime() {
        givenResponse(RESPUESTA);
        givenResponse("[]");
        adapter.buscar(CONSULTA, AHORA);
        tiempo.set(Duration.ofMillis(1100).toNanos());

        var resultados = adapter.buscar("Otra dirección", AHORA);

        assertThat(resultados).isEmpty();
        assertThatThrownBy(() -> adapter.buscar("Tercera dirección", AHORA.plusSeconds(100)))
                .isInstanceOf(ServicioUbicacionException.class);
    }

    @Test
    @DisplayName("La caché tiene un tamaño limitado y descarta la entrada más antigua")
    void buscarShouldEvictOldestEntryAtCapacity() {
        for (int indice = 0; indice <= NominatimGeocodificadorAdapter.MAXIMO_CACHE + 1; indice++) {
            givenResponse("[]");
        }

        for (int indice = 0; indice <= NominatimGeocodificadorAdapter.MAXIMO_CACHE; indice++) {
            buscar("Dirección " + indice, AHORA.plusSeconds(indice * 2L));
        }
        var resultados = buscar("Dirección 0", AHORA.plusSeconds(1000));

        assertThat(resultados).isEmpty();
    }

    @Test
    @DisplayName("Una caída del proveedor devuelve un error controlado sin filtrar la dirección ni su cuerpo")
    void buscarShouldHideUpstreamErrorDetails() {
        servidor.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("detalle privado"));

        assertThatThrownBy(() -> buscar(CONSULTA, AHORA))
                .isInstanceOf(ServicioUbicacionException.class)
                .hasMessage("No pudimos buscar la dirección. Probá de nuevo en un momento.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "no es json", "[null]", "[{}]",
            "[{\"display_name\":\" \" ,\"lat\":\"0\",\"lon\":\"0\"}]",
            "[{\"display_name\":\"Dirección\",\"lat\":\"91\",\"lon\":\"0\"}]",
            "[{\"display_name\":\"Dirección\",\"lat\":\"0\",\"lon\":\"181\"}]",
            "[{\"display_name\":\"Dirección\",\"lat\":\"NaN\",\"lon\":\"0\"}]"
    })
    @DisplayName("Una respuesta malformada del proveedor no llega al cliente como coordenadas válidas")
    void buscarShouldRejectMalformedResponse(String respuesta) {
        givenResponse(respuesta);

        assertThatThrownBy(() -> buscar(CONSULTA, AHORA))
                .isInstanceOf(ServicioUbicacionException.class);
    }

    // --- arrange ---
    private void givenResponse(String respuesta) {
        servidor.expect(anything()).andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
    }

    // --- act ---
    private List<DireccionEncontrada> buscar(String consulta, Instant instante) {
        tiempo.set(Duration.between(AHORA, instante).toNanos());
        return adapter.buscar(consulta, instante);
    }
}
