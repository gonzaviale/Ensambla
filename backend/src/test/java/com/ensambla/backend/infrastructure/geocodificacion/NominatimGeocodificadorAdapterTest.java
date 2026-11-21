package com.ensambla.backend.infrastructure.geocodificacion;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.ubicacion.DireccionEncontrada;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
            [{"display_name":"Varela 1903, San Justo","lat":"-34.6701","lon":"-58.5625",
              "address":{"road":"Varela","quarter":"Olivares","city":"San Justo","state":"Buenos Aires"}}]
            """;
    private static final BigDecimal LATITUD = new BigDecimal("-34.6701");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.5625");
    private static final String REVERSO = """
            {"display_name":"Olivares, San Justo","address":{"quarter":"Olivares","suburb":"Villa Luzuriaga",
             "city":"San Justo","state_district":"Partido de La Matanza","state":"Buenos Aires"}}
            """;
    private static final String SIN_RESULTADOS = "[]";

    private MockRestServiceServer servidor;
    private NominatimGeocodificadorAdapter adapter;
    private final AtomicLong tiempo = new AtomicLong();

    private List<DireccionEncontrada> resultados;
    private List<DireccionEncontrada> resultadosRepetidos;
    private LocalidadesDelPunto localidades;
    private LocalidadesDelPunto localidadesRepetidas;
    private Throwable rechazo;

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
    void buscaEnArgentinaConIdentificacionTextoEscapadoYLimiteDeCincoResultados() {
        dadoUnProveedorQueEsperaLaBusquedaIdentificada();

        cuandoSeBuscaLaDireccion();

        entoncesSeEncuentraLaDireccionConSusCoordenadasYLocalidad();
    }

    @Test
    @DisplayName("Una dirección sin detalle de localidad se devuelve sin localidad")
    void unaDireccionSinDetalleDeLocalidadSeDevuelveSinLocalidad() {
        dadoQueElProveedorResponde(
                "[{\"display_name\":\"Varela 1903\",\"lat\":\"-34.6701\",\"lon\":\"-58.5625\"}]");

        cuandoSeBuscaLaDireccion();

        entoncesLaDireccionNoTieneLocalidad();
    }

    @Test
    @DisplayName("Resuelve la localidad con geocodificación inversa del punto, sin contar barrios ni partidos")
    void resuelveLaLocalidadConGeocodificacionInversaDelPuntoSinContarBarriosNiPartidos() {
        dadoUnProveedorQueEsperaLaGeocodificacionInversaIdentificada();

        cuandoSeResuelveLaLocalidadDelPunto();

        entoncesLasLocalidadesSonSanJustoYVillaLuzuriaga();
    }

    @Test
    @DisplayName("Reutiliza la caché para el mismo punto aunque cambie la escala de las coordenadas")
    void reutilizaLaCacheParaElMismoPuntoAunqueCambieLaEscalaDeLasCoordenadas() {
        dadoQueElProveedorResponde(REVERSO);

        cuandoSeResuelveElMismoPuntoConOtraEscalaDeCoordenadas();

        entoncesSeReutilizaLaMismaRespuestaDeLocalidades();
    }

    @Test
    @DisplayName("Un punto que el proveedor no reconoce devuelve cero localidades")
    void unPuntoQueElProveedorNoReconoceDevuelveCeroLocalidades() {
        dadoQueElProveedorResponde("{\"error\":\"Unable to geocode\"}");

        cuandoSeResuelveLaLocalidadDelPunto();

        entoncesNoHayLocalidades();
    }

    @Test
    @DisplayName("Ignora nombres vacíos o repetidos del proveedor")
    void ignoraNombresVaciosORepetidosDelProveedor() {
        dadoQueElProveedorResponde("{\"address\":{\"city\":\"San Justo\",\"town\":\" \",\"suburb\":\"San Justo\"}}");

        cuandoSeResuelveLaLocalidadDelPunto();

        entoncesLaUnicaLocalidadEsSanJusto();
    }

    @Test
    @DisplayName("La búsqueda y la geocodificación inversa comparten la cuota global del proveedor")
    void laBusquedaYLaGeocodificacionInversaComparteLaCuotaGlobalDelProveedor() {
        dadoQueElProveedorResponde(RESPUESTA);

        cuandoSeBuscaYSeIntentaResolverUnPuntoAntesDelIntervalo();

        entoncesSeRechazaPorLaCuotaGlobal();
    }

    @Test
    @DisplayName("Una caída del proveedor al verificar la localidad no filtra coordenadas ni cuerpo")
    void unaCaidaDelProveedorAlVerificarLaLocalidadNoFiltraCoordenadasNiCuerpo() {
        dadoUnProveedorCaido();

        cuandoSeIntentaResolverLaLocalidadDelPunto();

        entoncesSeAvisaQueNoSePudoVerificarLaLocalidad();
    }

    @Test
    @DisplayName("Una respuesta vacía del proveedor al verificar la localidad es un error controlado")
    void unaRespuestaVaciaDelProveedorAlVerificarLaLocalidadEsUnErrorControlado() {
        dadoQueElProveedorResponde("");

        cuandoSeIntentaResolverLaLocalidadDelPunto();

        entoncesSeRechazaConUnErrorDeUbicacion();
    }

    @Test
    @DisplayName("Reutiliza la caché aunque la consulta cambie de mayúsculas")
    void reutilizaLaCacheAunqueLaConsultaCambieDeMayusculas() {
        dadoQueElProveedorResponde(RESPUESTA);

        cuandoSeBuscaLaMismaDireccionEnMayusculas();

        entoncesSeReutilizaLaMismaRespuestaDeBusqueda();
    }

    @Test
    @DisplayName("Las búsquedas vacías también se cachean")
    void lasBusquedasVaciasTambienSeCachean() {
        dadoQueElProveedorResponde(SIN_RESULTADOS);

        cuandoSeBuscaDosVecesUnaDireccionSinResultados();

        entoncesNoHayResultados();
    }

    @Test
    @DisplayName("La caché vence a las 24 horas y renueva la entrada")
    void laCacheVenceALas24HorasYRenuevaLaEntrada() {
        dadoQueElProveedorResponde(RESPUESTA);
        dadoQueElProveedorResponde(SIN_RESULTADOS);

        cuandoSeBuscaLaMismaDireccionAntesYDespuesDeQueVenzaLaCache();

        entoncesNoHayResultados();
    }

    @Test
    @DisplayName("Dos consultas distintas seguidas no exceden la cuota global del proveedor")
    void dosConsultasDistintasSeguidasNoExcedenLaCuotaGlobalDelProveedor() {
        dadoQueElProveedorResponde(RESPUESTA);

        cuandoSeBuscaYSeIntentaBuscarOtraDireccionAntesDelIntervalo();

        entoncesSeRechazaPorLaCuotaGlobal();
    }

    @Test
    @DisplayName("Una consulta nueva puede salir al terminar el intervalo mínimo")
    void unaConsultaNuevaPuedeSalirAlTerminarElIntervaloMinimo() {
        dadoQueElProveedorResponde(RESPUESTA);
        dadoQueElProveedorResponde(SIN_RESULTADOS);

        cuandoSeBuscaYLuegoOtraDireccionAlTerminarElIntervalo();

        entoncesNoHayResultados();
    }

    @Test
    @DisplayName("El límite usa el momento de salida aunque los pedidos traigan instantes anteriores")
    void elLimiteUsaElMomentoDeSalidaAunqueLosPedidosTraenInstantesAnteriores() {
        dadoQueElProveedorResponde(RESPUESTA);
        dadoQueElProveedorResponde(SIN_RESULTADOS);

        cuandoSeBuscaConElRelojAvanzandoMasQueElIntervalo();

        entoncesLaSegundaSaleYUnaTerceraInmediataSeRechaza();
    }

    @Test
    @DisplayName("La caché tiene un tamaño limitado y descarta la entrada más antigua")
    void laCacheTieneUnTamanioLimitadoYDescartaLaEntradaMasAntigua() {
        dadoQueElProveedorRespondeVacioUnaVezPorEntradaMasUna();

        cuandoSeBuscaUnaDireccionPorEntradaDeCacheYLuegoLaPrimeraDeNuevo();

        entoncesNoHayResultados();
    }

    @Test
    @DisplayName("Una caída del proveedor devuelve un error controlado sin filtrar la dirección ni su cuerpo")
    void unaCaidaDelProveedorDevuelveUnErrorControladoSinFiltrarLaDireccionNiSuCuerpo() {
        dadoUnProveedorCaido();

        cuandoSeIntentaBuscarLaDireccion();

        entoncesSeAvisaQueNoSePudoBuscarLaDireccion();
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
    void unaRespuestaMalformadaDelProveedorNoLlegaAlClienteComoCoordenadasValidas(String respuesta) {
        dadoQueElProveedorResponde(respuesta);

        cuandoSeIntentaBuscarLaDireccion();

        entoncesSeRechazaConUnErrorDeUbicacion();
    }

    // --- dado ---

    private void dadoQueElProveedorResponde(String respuesta) {
        servidor.expect(anything()).andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
    }

    private void dadoUnProveedorQueEsperaLaBusquedaIdentificada() {
        servidor.expect(requestTo(URL + "/search?q=Varela%201903%2C%20San%20Justo"
                        + "&format=jsonv2&limit=5&countrycodes=ar&accept-language=es&addressdetails=1"))
                .andExpect(header("User-Agent", "Ensambla-test"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
    }

    private void dadoUnProveedorQueEsperaLaGeocodificacionInversaIdentificada() {
        servidor.expect(requestTo(URL + "/reverse?lat=-34.670100&lon=-58.562500"
                        + "&format=jsonv2&zoom=14&addressdetails=1&accept-language=es"))
                .andExpect(header("User-Agent", "Ensambla-test"))
                .andRespond(withSuccess(REVERSO, MediaType.APPLICATION_JSON));
    }

    private void dadoUnProveedorCaido() {
        servidor.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("detalle privado"));
    }

    private void dadoQueElProveedorRespondeVacioUnaVezPorEntradaMasUna() {
        for (int indice = 0; indice <= NominatimGeocodificadorAdapter.MAXIMO_CACHE + 1; indice++) {
            dadoQueElProveedorResponde(SIN_RESULTADOS);
        }
    }

    // --- cuando ---

    private void cuandoSeBuscaLaDireccion() {
        resultados = buscar(CONSULTA, AHORA);
    }

    private void cuandoSeIntentaBuscarLaDireccion() {
        rechazo = catchThrowable(this::cuandoSeBuscaLaDireccion);
    }

    private void cuandoSeResuelveLaLocalidadDelPunto() {
        localidades = resolver(LATITUD, LONGITUD, AHORA);
    }

    private void cuandoSeIntentaResolverLaLocalidadDelPunto() {
        rechazo = catchThrowable(this::cuandoSeResuelveLaLocalidadDelPunto);
    }

    private void cuandoSeResuelveElMismoPuntoConOtraEscalaDeCoordenadas() {
        localidades = resolver(LATITUD, LONGITUD, AHORA);
        localidadesRepetidas = resolver(new BigDecimal("-34.670100"), new BigDecimal("-58.56250"), AHORA.plusMillis(1));
    }

    private void cuandoSeBuscaYSeIntentaResolverUnPuntoAntesDelIntervalo() {
        cuandoSeBuscaLaDireccion();
        rechazo = catchThrowable(() -> resolver(LATITUD, LONGITUD, AHORA.plusMillis(1099)));
    }

    private void cuandoSeBuscaLaMismaDireccionEnMayusculas() {
        resultados = buscar(CONSULTA, AHORA);
        resultadosRepetidos = buscar(CONSULTA.toUpperCase(), AHORA.plusMillis(1));
    }

    private void cuandoSeBuscaDosVecesUnaDireccionSinResultados() {
        buscar(CONSULTA, AHORA);
        resultados = buscar(CONSULTA, AHORA.plusSeconds(2));
    }

    private void cuandoSeBuscaLaMismaDireccionAntesYDespuesDeQueVenzaLaCache() {
        buscar(CONSULTA, AHORA);
        resultados = buscar(CONSULTA, AHORA.plusSeconds(86400));
    }

    private void cuandoSeBuscaYSeIntentaBuscarOtraDireccionAntesDelIntervalo() {
        cuandoSeBuscaLaDireccion();
        rechazo = catchThrowable(() -> buscar("Otra dirección", AHORA.plusMillis(1099)));
    }

    private void cuandoSeBuscaYLuegoOtraDireccionAlTerminarElIntervalo() {
        cuandoSeBuscaLaDireccion();
        resultados = buscar("Otra dirección", AHORA.plusMillis(1100));
    }

    private void cuandoSeBuscaConElRelojAvanzandoMasQueElIntervalo() {
        adapter.buscar(CONSULTA, AHORA);
        tiempo.set(Duration.ofMillis(1100).toNanos());
        resultados = adapter.buscar("Otra dirección", AHORA);
        rechazo = catchThrowable(() -> adapter.buscar("Tercera dirección", AHORA.plusSeconds(100)));
    }

    private void cuandoSeBuscaUnaDireccionPorEntradaDeCacheYLuegoLaPrimeraDeNuevo() {
        for (int indice = 0; indice <= NominatimGeocodificadorAdapter.MAXIMO_CACHE; indice++) {
            buscar("Dirección " + indice, AHORA.plusSeconds(indice * 2L));
        }
        resultados = buscar("Dirección 0", AHORA.plusSeconds(1000));
    }

    // --- entonces ---

    private void entoncesSeEncuentraLaDireccionConSusCoordenadasYLocalidad() {
        assertThat(resultados).hasSize(1);
        assertThat(resultados.getFirst().direccion()).isEqualTo(CONSULTA);
        assertThat(resultados.getFirst().latitud()).isEqualByComparingTo("-34.6701");
        assertThat(resultados.getFirst().longitud()).isEqualByComparingTo("-58.5625");
        assertThat(resultados.getFirst().localidad()).isEqualTo("San Justo");
    }

    private void entoncesLaDireccionNoTieneLocalidad() {
        assertThat(resultados.getFirst().localidad()).isNull();
    }

    private void entoncesLasLocalidadesSonSanJustoYVillaLuzuriaga() {
        assertThat(localidades.nombres()).containsExactly("San Justo", "Villa Luzuriaga");
        assertThat(localidades.principal()).contains("San Justo");
    }

    private void entoncesSeReutilizaLaMismaRespuestaDeLocalidades() {
        assertThat(localidadesRepetidas).isSameAs(localidades);
    }

    private void entoncesNoHayLocalidades() {
        assertThat(localidades.nombres()).isEmpty();
    }

    private void entoncesLaUnicaLocalidadEsSanJusto() {
        assertThat(localidades.nombres()).containsExactly("San Justo");
    }

    private void entoncesSeRechazaPorLaCuotaGlobal() {
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class).hasMessageContaining("Esperá un segundo");
    }

    private void entoncesSeAvisaQueNoSePudoVerificarLaLocalidad() {
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class)
                .hasMessage("No pudimos verificar la localidad. Probá de nuevo en un momento.");
    }

    private void entoncesSeAvisaQueNoSePudoBuscarLaDireccion() {
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class)
                .hasMessage("No pudimos buscar la dirección. Probá de nuevo en un momento.");
    }

    private void entoncesSeRechazaConUnErrorDeUbicacion() {
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class);
    }

    private void entoncesSeReutilizaLaMismaRespuestaDeBusqueda() {
        assertThat(resultadosRepetidos).isSameAs(resultados);
    }

    private void entoncesNoHayResultados() {
        assertThat(resultados).isEmpty();
    }

    private void entoncesLaSegundaSaleYUnaTerceraInmediataSeRechaza() {
        assertThat(resultados).isEmpty();
        assertThat(rechazo).isInstanceOf(ServicioUbicacionException.class);
    }

    // --- datos ---

    private LocalidadesDelPunto resolver(BigDecimal latitud, BigDecimal longitud, Instant instante) {
        tiempo.set(Duration.between(AHORA, instante).toNanos());
        return adapter.resolverLocalidades(latitud, longitud, instante);
    }

    private List<DireccionEncontrada> buscar(String consulta, Instant instante) {
        tiempo.set(Duration.between(AHORA, instante).toNanos());
        return adapter.buscar(consulta, instante);
    }
}
