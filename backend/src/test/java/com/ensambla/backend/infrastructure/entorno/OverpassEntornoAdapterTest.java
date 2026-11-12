package com.ensambla.backend.infrastructure.entorno;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Punto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OverpassEntornoAdapterTest {

    private static final String URL = "https://overpass.test";
    private static final Instant AHORA = Instant.parse("2026-11-12T12:00:00Z");
    private static final BigDecimal LATITUD = new BigDecimal("-34.67030");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.56280");
    private static final String RESPUESTA = """
            {"elements":[
              {"type":"node","id":1,"lat":-34.6701,"lon":-58.5625,"tags":{"amenity":"school","name":"Escuela 1"}},
              {"type":"way","id":2,"center":{"lat":-34.6702,"lon":-58.5626},"tags":{"office":"company"}},
              {"type":"node","id":3,"lat":-34.6703,"lon":-58.5627,"tags":{"highway":"bus_stop"}},
              {"type":"node","id":4,"lat":-34.6704,"lon":-58.5628,"tags":{"railway":"station","name":"Estación"}},
              {"type":"node","id":5,"lat":-34.6705,"lon":-58.5629,"tags":{"public_transport":"platform"}},
              {"type":"way","id":6,"center":{"lat":-34.6706,"lon":-58.5630},"tags":{"leisure":"park","name":"Plaza"}},
              {"type":"node","id":7,"lat":-34.6707,"lon":-58.5631,"tags":{"amenity":"clinic"}},
              {"type":"node","id":8,"lat":-34.6708,"lon":-58.5632,"tags":{"amenity":"pharmacy"}},
              {"type":"node","id":9,"lat":-34.6709,"lon":-58.5633,"tags":{"shop":"supermarket","name":"Súper"}},
              {"type":"node","id":10,"lat":-34.6710,"lon":-58.5634,"tags":{"shop":"convenience"}},
              {"type":"node","id":11,"lat":-34.6711,"lon":-58.5635,"tags":{"shop":"kiosk"}},
              {"type":"node","id":12,"lat":-34.6712,"lon":-58.5636,"tags":{"shop":"stationery"}},
              {"type":"node","id":13,"tags":{"amenity":"school"}},
              {"type":"way","id":14,"tags":{"amenity":"school"}},
              {"type":"node","id":17,"lat":-34.6,"tags":{"amenity":"school"}},
              {"type":"node","id":15,"lat":-34.6713,"lon":-58.5637},
              {"type":"node","id":16,"lat":-34.6714,"lon":-58.5638,"tags":{"shop":"clothes"}}
            ]}
            """;

    private MockRestServiceServer servidor;
    private OverpassEntornoAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL).defaultHeader("User-Agent", "Ensambla-test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new OverpassEntornoAdapter(builder.build());
    }

    @AfterEach
    void tearDown() {
        servidor.verify();
    }

    @Test
    @DisplayName("Pide a 500 m del punto, con identificación, y clasifica con las mismas reglas del snapshot")
    void consultarShouldClassifyPlacesAroundThePoint() {
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL + "/api/interpreter?data=")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("User-Agent", "Ensambla-test"))
                .andExpect(solicitud -> assertThat(java.net.URLDecoder.decode(
                        solicitud.getURI().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8))
                        .contains("(around:500,-34.67030,-58.56280)", "out center tags"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        List<Punto> puntos = adapter.consultar(LATITUD, LONGITUD, AHORA);

        assertThat(cuenta(puntos, "ESCUELA", null)).isEqualTo(1);
        assertThat(cuenta(puntos, "OFICINA", null)).isEqualTo(1);
        assertThat(cuenta(puntos, "TRANSPORTE", null)).isEqualTo(3);
        assertThat(cuenta(puntos, "PARQUE", null)).isEqualTo(1);
        assertThat(cuenta(puntos, "SALUD", null)).isEqualTo(1);
        assertThat(cuenta(puntos, "COMPLEMENTARIO", null)).isEqualTo(2);
        assertThat(cuenta(puntos, "COMPETENCIA", "ALMACEN")).isEqualTo(2);
        assertThat(cuenta(puntos, "COMPETENCIA", "KIOSCO")).isEqualTo(2);
        assertThat(cuenta(puntos, "COMPETENCIA", "PAPELERIA")).isEqualTo(1);
        assertThat(puntos).hasSize(14);
    }

    @Test
    @DisplayName("Conserva nombre, coordenadas, fuente y fecha; usa el centro de las áreas y nombra lo anónimo")
    void consultarShouldKeepTraceableDetails() {
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        List<Punto> puntos = adapter.consultar(LATITUD, LONGITUD, AHORA);

        Punto escuela = puntos.getFirst();
        assertThat(escuela.clave()).isEqualTo("node:1");
        assertThat(escuela.nombre()).isEqualTo("Escuela 1");
        assertThat(escuela.latitud()).isEqualByComparingTo("-34.6701");
        assertThat(escuela.longitud()).isEqualByComparingTo("-58.5625");
        assertThat(escuela.fuente().codigo()).isEqualTo("OSM");
        assertThat(escuela.fuente().licencia()).isEqualTo("ODbL 1.0");
        assertThat(escuela.observadaEn()).isEqualTo(LocalDate.parse("2026-11-12"));
        Punto oficina = puntos.get(1);
        assertThat(oficina.clave()).isEqualTo("way:2");
        assertThat(oficina.nombre()).isEqualTo("Lugar sin nombre");
        assertThat(oficina.latitud()).isEqualByComparingTo("-34.6702");
    }

    @Test
    @DisplayName("Repite la respuesta desde la caché durante un día y vuelve a consultar al vencer")
    void consultarShouldCacheForOneDay() {
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess("{\"elements\":[]}", MediaType.APPLICATION_JSON));
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        adapter.consultar(LATITUD, LONGITUD, AHORA);
        List<Punto> repetida = adapter.consultar(new BigDecimal("-34.67031"), LONGITUD, AHORA.plusSeconds(3_600));
        List<Punto> vencida = adapter.consultar(LATITUD, LONGITUD, AHORA.plusSeconds(86_401));

        assertThat(repetida).isEmpty();
        assertThat(vencida).hasSize(14);
    }

    @Test
    @DisplayName("Descarta la entrada más antigua cuando la caché se llena")
    void consultarShouldEvictOldestCachedPoint() {
        for (int i = 0; i <= OverpassEntornoAdapter.MAXIMO_CACHE; i++) {
            servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                            .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                    .andRespond(withSuccess("{\"elements\":[]}", MediaType.APPLICATION_JSON));
        }
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess("{\"elements\":[]}", MediaType.APPLICATION_JSON));

        for (int i = 0; i <= OverpassEntornoAdapter.MAXIMO_CACHE; i++) {
            adapter.consultar(new BigDecimal("-34.6").add(new BigDecimal(i).movePointLeft(3)), LONGITUD, AHORA);
        }
        adapter.consultar(new BigDecimal("-34.6"), LONGITUD, AHORA);
    }

    @Test
    @DisplayName("Si la fuente falla o responde vacío, avisa sin exponer coordenadas")
    void consultarShouldHideProviderFailures() {
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.consultar(LATITUD, LONGITUD, AHORA))
                .isInstanceOf(ServicioUbicacionException.class)
                .hasMessage("No pudimos medir ese lugar ahora. Probá de nuevo en un momento.")
                .hasMessageNotContaining("-34");
        assertThatThrownBy(() -> adapter.consultar(LATITUD, LONGITUD, AHORA))
                .isInstanceOf(ServicioUbicacionException.class);
    }

    @Test
    @DisplayName("Una respuesta sin elementos cuenta como un entorno vacío, no como un error")
    void consultarShouldTreatMissingElementsAsEmptySurroundings() {
        servidor.expect(once(), org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo(org.hamcrest.Matchers.startsWith(URL)))
                .andRespond(withSuccess("{\"remark\":\"runtime error\"}", MediaType.APPLICATION_JSON));

        assertThat(adapter.consultar(LATITUD, LONGITUD, AHORA)).isEmpty();
    }

    private static long cuenta(List<Punto> puntos, String tipo, String rubro) {
        return puntos.stream()
                .filter(punto -> tipo.equals(punto.tipo()) && (rubro == null || rubro.equals(punto.rubro())))
                .count();
    }
}
