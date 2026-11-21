package com.ensambla.evaluacion;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LecturaEvaluacionTest {

    private static final String[] ARGUMENTOS_DEL_COMANDO_SIMULADO = {
            "--ensambla.ia.proveedor=simulado", "--spring.config.import=",
            "--spring.main.banner-mode=off", "--logging.level.root=WARN"};

    @TempDir
    private Path carpeta;

    private Path entrada;
    private Path salida;
    private LectorCuadernoPort lector;
    private LecturaEvaluacion.DestinoEvaluacion destino;
    private JsonNode resultado;
    private LecturaEvaluacion.ResultadoEvaluacion resultadoSinColumnasNuevas;

    @Test
    @DisplayName("El comando real usa el mismo lector simulado sin levantar servidor ni base")
    void elComandoRealUsaElMismoLectorSimuladoSinLevantarServidorNiBase() throws Exception {
        dadoUnaSolicitudSinDestino();

        cuandoSeCorreElComandoReal();

        entoncesLaLecturaSimuladaSaleDelComando();
    }

    @Test
    @DisplayName("Registra fallos de extracción por caso sin producir lecturas ficticias")
    void registraFallosDeExtraccionPorCasoSinProducirLecturasFicticias() throws Exception {
        dadoUnaSolicitudSinDestino();
        dadoUnLectorQueFalla();

        cuandoSeEjecutaLaEvaluacionSinDestino();

        entoncesElCasoQuedaConElErrorYSinLectura();
    }

    @Test
    @DisplayName("Con un bloque destino se devuelven las columnas que el bloque no tiene")
    void conUnBloqueDestinoSeDevuelvenLasColumnasQueElBloqueNoTiene() throws Exception {
        dadoUnLectorQueLeeLasColumnasMontoYTelefono();
        dadoUnDestinoVentasConMontoYEstado();

        cuandoSeEjecutaLaEvaluacionConDestino();

        entoncesSoloLaColumnaTelefonoSaleComoNueva();
    }

    @Test
    @DisplayName("Con un bloque destino la IA ve sólo ese bloque")
    void conUnBloqueDestinoLaIaVeSoloEseBloque() throws Exception {
        dadoUnLectorQueLeeLasColumnasMontoYTelefono();
        dadoUnDestinoVentasConMontoYEstado();

        cuandoSeEjecutaLaEvaluacionConDestino();

        entoncesLaIaVioSoloElBloqueDestinoConSusCampos();
    }

    @Test
    @DisplayName("Con destino, una columna parecida a un campo del bloque no se cuenta como nueva")
    void conDestinoUnaColumnaParecidaAUnCampoDelBloqueNoSeCuentaComoNueva() throws Exception {
        dadoUnLectorQueLeeLaColumnaTelefono();
        dadoUnDestinoClientesConCelular();

        cuandoSeEjecutaLaEvaluacionConDestino();

        entoncesNoHayColumnasNuevasYLaLecturaUsaElCampoCelular();
    }

    @Test
    @DisplayName("Con destino, una foto que no coincide con ninguna columna del bloque se informa como error")
    void conDestinoUnaFotoQueNoCoincideConNingunaColumnaDelBloqueSeInformaComoError() throws Exception {
        dadoUnLectorQueLeeLaColumnaColor();
        dadoUnDestinoVentasConSoloMonto();

        cuandoSeEjecutaLaEvaluacionConDestino();

        entoncesLaFotoSeInformaComoNoCompatible();
    }

    @Test
    @DisplayName("El comando real con el lector simulado detecta como nueva la columna que el bloque no tiene")
    void elComandoRealConElLectorSimuladoDetectaComoNuevaLaColumnaQueElBloqueNoTiene() throws Exception {
        dadoUnaSolicitudConDestinoDelEjemploSimulado();

        cuandoSeCorreElComandoReal();

        entoncesLaColumnaPagoSaleComoNueva();
    }

    @Test
    @DisplayName("Un resultado sin columnas nuevas informadas las deja vacías")
    void unResultadoSinColumnasNuevasInformadasLasDejaVacias() {
        cuandoSeArmaUnResultadoSinColumnasNuevas();

        entoncesLasColumnasNuevasQuedanVacias();
    }

    // --- dado ---

    private void dadoUnaSolicitudSinDestino() throws Exception {
        Path foto = escribirUnaFoto("sintetico.jpg");
        entrada = carpeta.resolve("entrada.json");
        salida = carpeta.resolve("salida.json");
        escribirLaSolicitud(new LecturaEvaluacion.CasoEvaluacion("sintetico-001", foto.toString(), "image/jpeg"));
    }

    private void dadoUnaSolicitudConDestinoDelEjemploSimulado() throws Exception {
        Path foto = escribirUnaFoto("sintetico.jpg");
        entrada = carpeta.resolve("entrada-destino.json");
        salida = carpeta.resolve("salida-destino.json");
        destino = destino("Ventas", campo("Fecha", "FECHA", null), campo("Cliente", "TEXTO", null),
                campo("Producto", "OPCION", List.of("Vela grande", "Vela chica", "Difusor")),
                campo("Cantidad", "NUMERO", null), campo("Importe", "NUMERO", null));
        escribirLaSolicitud(
                new LecturaEvaluacion.CasoEvaluacion("con-destino", foto.toString(), "image/jpeg", destino));
    }

    private void dadoUnLectorQueFalla() {
        lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla"));
    }

    private void dadoUnLectorQueLeeLasColumnasMontoYTelefono() {
        dadoUnLectorQueLee(columna("monto", "Monto", TipoCampo.NUMERO),
                columna("telefono", "Teléfono", TipoCampo.TEXTO));
    }

    private void dadoUnLectorQueLeeLaColumnaTelefono() {
        dadoUnLectorQueLee(columna("telefono", "Teléfono", TipoCampo.TEXTO));
    }

    private void dadoUnLectorQueLeeLaColumnaColor() {
        dadoUnLectorQueLee(columna("color", "Color", TipoCampo.TEXTO));
    }

    private void dadoUnLectorQueLee(CampoPropuesto... columnas) {
        lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenReturn(unaLecturaCon(columnas));
    }

    private void dadoUnDestinoVentasConMontoYEstado() {
        destino = destino("Ventas", campo("Monto", "NUMERO", null),
                campo("Estado", "OPCION", List.of("Pagado", "Debe")));
    }

    private void dadoUnDestinoClientesConCelular() {
        destino = destino("Clientes", campo("Celular", "TEXTO", List.of()));
    }

    private void dadoUnDestinoVentasConSoloMonto() {
        destino = destino("Ventas", campo("Monto", "NUMERO", null));
    }

    // --- cuando ---

    private void cuandoSeCorreElComandoReal() throws Exception {
        List<String> argumentos = new ArrayList<>(List.of(
                "--evaluacion.entrada=" + entrada, "--evaluacion.salida=" + salida));
        argumentos.addAll(List.of(ARGUMENTOS_DEL_COMANDO_SIMULADO));
        EvaluacionIaApplication.main(argumentos.toArray(String[]::new));
        resultado = leerLaSalida();
    }

    private void cuandoSeEjecutaLaEvaluacionSinDestino() throws Exception {
        new LecturaEvaluacion(lector, JsonMapper.builder().build()).ejecutar(entrada, salida, Duration.ofSeconds(5));
        resultado = leerLaSalida();
    }

    private void cuandoSeEjecutaLaEvaluacionConDestino() throws Exception {
        Path foto = escribirUnaFoto("sintetico-destino.jpg");
        entrada = carpeta.resolve("entrada-mock.json");
        salida = carpeta.resolve("salida-mock.json");
        escribirLaSolicitud(
                new LecturaEvaluacion.CasoEvaluacion("con-destino", foto.toString(), "image/jpeg", destino));
        cuandoSeEjecutaLaEvaluacionSinDestino();
    }

    private void cuandoSeArmaUnResultadoSinColumnasNuevas() {
        resultadoSinColumnasNuevas = new LecturaEvaluacion.ResultadoEvaluacion("caso", null, "Falla", null);
    }

    // --- entonces ---

    private void entoncesLaLecturaSimuladaSaleDelComando() {
        assertThat(resultado.path("id").asString()).isEqualTo("sintetico-001");
        assertThat(resultado.path("lectura").path("tipoRegistro").asString()).isEqualTo("VENTAS");
        assertThat(resultado.path("lectura").path("consumo").path("proveedor").asString()).isEqualTo("simulado");
    }

    private void entoncesElCasoQuedaConElErrorYSinLectura() {
        assertThat(resultado.path("error").asString()).isEqualTo("Falla");
        assertThat(resultado.path("lectura").isNull()).isTrue();
    }

    private void entoncesSoloLaColumnaTelefonoSaleComoNueva() {
        assertThat(resultado.path("error").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).hasSize(1);
        assertThat(resultado.path("camposNuevos").get(0).path("nombre").asString()).isEqualTo("Teléfono");
        assertThat(resultado.path("camposNuevos").get(0).path("tipo").asString()).isEqualTo("TEXTO");
    }

    @SuppressWarnings("unchecked")
    private void entoncesLaIaVioSoloElBloqueDestinoConSusCampos() {
        ArgumentCaptor<List<BloqueConocido>> bloques = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<BloqueId> destinoId = ArgumentCaptor.forClass(BloqueId.class);
        verify(lector).leer(any(), bloques.capture(), destinoId.capture());
        assertThat(bloques.getValue()).hasSize(1);
        assertThat(bloques.getValue().getFirst().id()).isEqualTo(destinoId.getValue());
        assertThat(bloques.getValue().getFirst().campos()).extracting("nombre").containsExactly("Monto", "Estado");
        assertThat(bloques.getValue().getFirst().campos().get(1).opciones()).containsExactly("Pagado", "Debe");
    }

    private void entoncesNoHayColumnasNuevasYLaLecturaUsaElCampoCelular() {
        assertThat(resultado.path("camposNuevos")).isEmpty();
        assertThat(resultado.path("lectura").path("bloque").path("campos").get(0).path("nombre").asString())
                .isEqualTo("Celular");
    }

    private void entoncesLaFotoSeInformaComoNoCompatible() {
        assertThat(resultado.path("error").asString()).contains("compatible");
        assertThat(resultado.path("lectura").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).isEmpty();
    }

    private void entoncesLaColumnaPagoSaleComoNueva() {
        assertThat(resultado.path("error").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).hasSize(1);
        assertThat(resultado.path("camposNuevos").get(0).path("nombre").asString()).isEqualTo("¿Pagó?");
        assertThat(resultado.path("camposNuevos").get(0).path("tipo").asString()).isEqualTo("SI_NO");
    }

    private void entoncesLasColumnasNuevasQuedanVacias() {
        assertThat(resultadoSinColumnasNuevas.camposNuevos()).isEmpty();
    }

    // --- datos ---

    private Path escribirUnaFoto(String nombre) throws Exception {
        Path foto = carpeta.resolve(nombre);
        Files.write(foto, new byte[]{1, 2, 3});
        return foto;
    }

    private void escribirLaSolicitud(LecturaEvaluacion.CasoEvaluacion caso) throws Exception {
        var solicitud = new LecturaEvaluacion.SolicitudEvaluacion(List.of(caso));
        Files.writeString(entrada, JsonMapper.builder().build().writeValueAsString(solicitud));
    }

    private JsonNode leerLaSalida() throws Exception {
        return JsonMapper.builder().build().readTree(Files.readString(salida)).get(0);
    }

    private LecturaEvaluacion.DestinoEvaluacion destino(String nombre, LecturaEvaluacion.CampoDestino... campos) {
        return new LecturaEvaluacion.DestinoEvaluacion(nombre, List.of(campos));
    }

    private LecturaEvaluacion.CampoDestino campo(String nombre, String tipo, List<String> opciones) {
        return new LecturaEvaluacion.CampoDestino(nombre, tipo, opciones);
    }

    private CampoPropuesto columna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    private LecturaCuaderno unaLecturaCon(CampoPropuesto... campos) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>();
        for (CampoPropuesto campo : campos) {
            datos.put(campo.clave(), new DatoLeido("1", 0.9));
        }
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, List.of(campos)),
                List.of(new FilaLeida(1, 1, datos)), List.of(), new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1));
    }
}
