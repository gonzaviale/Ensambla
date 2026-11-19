package com.ensambla.backend.infrastructure.entorno;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.zona.DatosZonaDondeAbrir.Punto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OverpassEntornoAdapterTest {

    private static final String URL = "https://overpass.test";
    private static final Instant AHORA = Instant.parse("2026-11-12T12:00:00Z");
    private static final BigDecimal LATITUD = new BigDecimal("-34.67030");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.56280");
    private static final String SIN_ELEMENTOS = "{\"elements\":[]}";
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
    private List<Punto> puntos;
    private List<Punto> puntosRepetidos;
    private List<Punto> puntosVencidos;
    private Throwable fallo;

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
    void pideA500MDelPuntoConIdentificacionYClasificaConLasMismasReglasDelSnapshot() {
        dadoUnOverpassQueEsperaLaConsultaIdentificadaA500Metros();

        cuandoSeConsultaElPunto();

        entoncesSeClasificanLosLugaresDelEntorno();
    }

    @Test
    @DisplayName("Conserva nombre, coordenadas, fuente y fecha; usa el centro de las áreas y nombra lo anónimo")
    void conservaNombreCoordenadasFuenteYFechaUsaElCentroDeLasAreasYNombraLoAnonimo() {
        dadoQueOverpassResponde(RESPUESTA);

        cuandoSeConsultaElPunto();

        entoncesLaEscuelaConservaSusDatosTrazables();
        entoncesLaOficinaUsaElCentroDelAreaYSeNombraSinNombre();
    }

    @Test
    @DisplayName("Repite la respuesta desde la caché durante un día y vuelve a consultar al vencer")
    void repiteLaRespuestaDesdeLaCacheDuranteUnDiaYVuelveAConsultarAlVencer() {
        dadoQueOverpassResponde(SIN_ELEMENTOS);
        dadoQueOverpassResponde(RESPUESTA);

        cuandoSeConsultaElMismoPuntoAntesYDespuesDeQueVenzaLaCache();

        entoncesLaRepetidaSaleVaciaDeLaCacheYLaVencidaTraeLosCatorcePuntos();
    }

    @Test
    @DisplayName("Descarta la entrada más antigua cuando la caché se llena")
    void descartaLaEntradaMasAntiguaCuandoLaCacheSeLlena() {
        dadoQueOverpassResponderaUnaVezPorPuntoMasUnaExtra();

        cuandoSeConsultaUnPuntoPorEntradaDeCacheYLuegoElPrimeroDeNuevo();

        entoncesSeVolvioAConsultarElPrimerPuntoPorqueSeDescarto();
    }

    @Test
    @DisplayName("Si la fuente falla, avisa sin exponer coordenadas")
    void siLaFuenteFallaAvisaSinExponerCoordenadas() {
        dadoQueOverpassResponde(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        cuandoSeIntentaConsultarElPunto();

        entoncesSeAvisaSinExponerCoordenadas();
    }

    @Test
    @DisplayName("Si la fuente responde vacío, es un error controlado")
    void siLaFuenteRespondeVacioEsUnErrorControlado() {
        dadoQueOverpassResponde("null");

        cuandoSeIntentaConsultarElPunto();

        entoncesSeRechazaConUnErrorDeUbicacion();
    }

    @Test
    @DisplayName("Una respuesta sin elementos cuenta como un entorno vacío, no como un error")
    void unaRespuestaSinElementosCuentaComoUnEntornoVacioNoComoUnError() {
        dadoQueOverpassResponde("{\"remark\":\"runtime error\"}");

        cuandoSeConsultaElPunto();

        entoncesElEntornoEstaVacio();
    }

    // --- dado ---

    private void dadoUnOverpassQueEsperaLaConsultaIdentificadaA500Metros() {
        servidor.expect(once(), requestTo(startsWith(URL + "/api/interpreter?data=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "Ensambla-test"))
                .andExpect(solicitud -> assertThat(URLDecoder.decode(
                        solicitud.getURI().getRawQuery(), StandardCharsets.UTF_8))
                        .contains("(around:500,-34.67030,-58.56280)", "out center tags"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
    }

    private void dadoQueOverpassResponde(String cuerpo) {
        dadoQueOverpassResponde(withSuccess(cuerpo, MediaType.APPLICATION_JSON));
    }

    private void dadoQueOverpassResponde(ResponseCreator respuesta) {
        servidor.expect(once(), requestTo(startsWith(URL))).andRespond(respuesta);
    }

    private void dadoQueOverpassResponderaUnaVezPorPuntoMasUnaExtra() {
        for (int i = 0; i <= OverpassEntornoAdapter.MAXIMO_CACHE + 1; i++) {
            dadoQueOverpassResponde(SIN_ELEMENTOS);
        }
    }

    // --- cuando ---

    private void cuandoSeConsultaElPunto() {
        puntos = adapter.consultar(LATITUD, LONGITUD, AHORA);
    }

    private void cuandoSeIntentaConsultarElPunto() {
        fallo = catchThrowable(this::cuandoSeConsultaElPunto);
    }

    private void cuandoSeConsultaElMismoPuntoAntesYDespuesDeQueVenzaLaCache() {
        adapter.consultar(LATITUD, LONGITUD, AHORA);
        puntosRepetidos = adapter.consultar(new BigDecimal("-34.67031"), LONGITUD, AHORA.plusSeconds(3_600));
        puntosVencidos = adapter.consultar(LATITUD, LONGITUD, AHORA.plusSeconds(86_401));
    }

    private void cuandoSeConsultaUnPuntoPorEntradaDeCacheYLuegoElPrimeroDeNuevo() {
        for (int i = 0; i <= OverpassEntornoAdapter.MAXIMO_CACHE; i++) {
            adapter.consultar(new BigDecimal("-34.6").add(new BigDecimal(i).movePointLeft(3)), LONGITUD, AHORA);
        }
        adapter.consultar(new BigDecimal("-34.6"), LONGITUD, AHORA);
    }

    // --- entonces ---

    private void entoncesSeClasificanLosLugaresDelEntorno() {
        assertThat(cuenta("ESCUELA", null)).isEqualTo(1);
        assertThat(cuenta("OFICINA", null)).isEqualTo(1);
        assertThat(cuenta("TRANSPORTE", null)).isEqualTo(3);
        assertThat(cuenta("PARQUE", null)).isEqualTo(1);
        assertThat(cuenta("SALUD", null)).isEqualTo(1);
        assertThat(cuenta("COMPLEMENTARIO", null)).isEqualTo(2);
        assertThat(cuenta("COMPETENCIA", "ALMACEN")).isEqualTo(2);
        assertThat(cuenta("COMPETENCIA", "KIOSCO")).isEqualTo(2);
        assertThat(cuenta("COMPETENCIA", "PAPELERIA")).isEqualTo(1);
        assertThat(puntos).hasSize(14);
    }

    private void entoncesLaEscuelaConservaSusDatosTrazables() {
        Punto escuela = puntos.getFirst();
        assertThat(escuela.clave()).isEqualTo("node:1");
        assertThat(escuela.nombre()).isEqualTo("Escuela 1");
        assertThat(escuela.latitud()).isEqualByComparingTo("-34.6701");
        assertThat(escuela.longitud()).isEqualByComparingTo("-58.5625");
        assertThat(escuela.fuente().codigo()).isEqualTo("OSM");
        assertThat(escuela.fuente().licencia()).isEqualTo("ODbL 1.0");
        assertThat(escuela.observadaEn()).isEqualTo(LocalDate.parse("2026-11-12"));
    }

    private void entoncesLaOficinaUsaElCentroDelAreaYSeNombraSinNombre() {
        Punto oficina = puntos.get(1);
        assertThat(oficina.clave()).isEqualTo("way:2");
        assertThat(oficina.nombre()).isEqualTo("Lugar sin nombre");
        assertThat(oficina.latitud()).isEqualByComparingTo("-34.6702");
    }

    private void entoncesLaRepetidaSaleVaciaDeLaCacheYLaVencidaTraeLosCatorcePuntos() {
        assertThat(puntosRepetidos).isEmpty();
        assertThat(puntosVencidos).hasSize(14);
    }

    private void entoncesSeVolvioAConsultarElPrimerPuntoPorqueSeDescarto() {
        servidor.verify();
    }

    private void entoncesSeAvisaSinExponerCoordenadas() {
        assertThat(fallo)
                .isInstanceOf(ServicioUbicacionException.class)
                .hasMessage("No pudimos medir ese lugar ahora. Probá de nuevo en un momento.")
                .hasMessageNotContaining("-34");
    }

    private void entoncesSeRechazaConUnErrorDeUbicacion() {
        assertThat(fallo).isInstanceOf(ServicioUbicacionException.class);
    }

    private void entoncesElEntornoEstaVacio() {
        assertThat(puntos).isEmpty();
    }

    // --- datos ---

    private long cuenta(String tipo, String rubro) {
        return puntos.stream()
                .filter(punto -> tipo.equals(punto.tipo()) && (rubro == null || rubro.equals(punto.rubro())))
                .count();
    }
}
