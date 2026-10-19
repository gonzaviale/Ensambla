package com.ensambla.evaluacion;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LecturaEvaluacionTest {

    @TempDir
    private Path carpeta;

    @Test
    @DisplayName("El comando real usa el mismo lector simulado sin levantar servidor ni base")
    void mainShouldRunExtractionWithoutDatabase() throws Exception {
        Path entrada = givenSolicitud();
        Path salida = carpeta.resolve("salida.json");

        EvaluacionIaApplication.main(new String[]{
                "--evaluacion.entrada=" + entrada, "--evaluacion.salida=" + salida,
                "--ensambla.ia.proveedor=simulado", "--spring.config.import=",
                "--spring.main.banner-mode=off", "--logging.level.root=WARN"});

        var resultado = JsonMapper.builder().build().readTree(Files.readString(salida));
        assertThat(resultado.get(0).path("id").asString()).isEqualTo("sintetico-001");
        assertThat(resultado.get(0).path("lectura").path("tipoRegistro").asString()).isEqualTo("VENTAS");
        assertThat(resultado.get(0).path("lectura").path("consumo").path("proveedor").asString()).isEqualTo("simulado");
    }

    @Test
    @DisplayName("Registra fallos de extracción por caso sin producir lecturas ficticias")
    void ejecutarShouldRecordExtractionError() throws Exception {
        Path entrada = givenSolicitud();
        Path salida = carpeta.resolve("salida.json");
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla"));
        var evaluacion = new LecturaEvaluacion(lector, JsonMapper.builder().build());

        evaluacion.ejecutar(entrada, salida, Duration.ofSeconds(5));

        var resultado = JsonMapper.builder().build().readTree(Files.readString(salida));
        assertThat(resultado.get(0).path("error").asString()).isEqualTo("Falla");
        assertThat(resultado.get(0).path("lectura").isNull()).isTrue();
    }

    @Test
    @DisplayName("Con un bloque destino la IA ve sólo ese bloque y se devuelven las columnas que no tiene")
    void ejecutarShouldDetectNewColumnsInTheDestinationBlock() throws Exception {
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenReturn(givenLectura(
                columna("monto", "Monto", TipoCampo.NUMERO), columna("telefono", "Teléfono", TipoCampo.TEXTO)));

        var resultado = ejecutarConDestino(lector, destino("Ventas",
                campo("Monto", "NUMERO", null), campo("Estado", "OPCION", List.of("Pagado", "Debe"))));

        assertThat(resultado.path("error").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).hasSize(1);
        assertThat(resultado.path("camposNuevos").get(0).path("nombre").asString()).isEqualTo("Teléfono");
        assertThat(resultado.path("camposNuevos").get(0).path("tipo").asString()).isEqualTo("TEXTO");
        ArgumentCaptor<List<BloqueConocido>> bloques = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<UUID> destinoId = ArgumentCaptor.forClass(UUID.class);
        verify(lector).leer(any(), bloques.capture(), destinoId.capture());
        assertThat(bloques.getValue()).hasSize(1);
        assertThat(bloques.getValue().getFirst().id()).isEqualTo(destinoId.getValue());
        assertThat(bloques.getValue().getFirst().campos()).extracting("nombre").containsExactly("Monto", "Estado");
        assertThat(bloques.getValue().getFirst().campos().get(1).opciones()).containsExactly("Pagado", "Debe");
    }

    @Test
    @DisplayName("Con destino, una columna parecida a un campo del bloque no se cuenta como nueva")
    void ejecutarShouldNotDuplicateColumnsThatTheBlockAlreadyHas() throws Exception {
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenReturn(givenLectura(
                columna("telefono", "Teléfono", TipoCampo.TEXTO)));

        var resultado = ejecutarConDestino(lector, destino("Clientes", campo("Celular", "TEXTO", List.of())));

        assertThat(resultado.path("camposNuevos")).isEmpty();
        assertThat(resultado.path("lectura").path("bloque").path("campos").get(0).path("nombre").asString())
                .isEqualTo("Celular");
    }

    @Test
    @DisplayName("Con destino, una foto que no coincide con ninguna columna del bloque se informa como error")
    void ejecutarShouldReportPhotoThatIsNotFromTheDestination() throws Exception {
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenReturn(givenLectura(columna("color", "Color", TipoCampo.TEXTO)));

        var resultado = ejecutarConDestino(lector, destino("Ventas", campo("Monto", "NUMERO", null)));

        assertThat(resultado.path("error").asString()).contains("compatible");
        assertThat(resultado.path("lectura").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).isEmpty();
    }

    @Test
    @DisplayName("El comando real con el lector simulado detecta como nueva la columna que el bloque no tiene")
    void mainShouldDetectNewColumnWithSimulatedReader() throws Exception {
        Path foto = carpeta.resolve("sintetico.jpg");
        Files.write(foto, new byte[]{1, 2, 3});
        Path entrada = carpeta.resolve("entrada-destino.json");
        Path salida = carpeta.resolve("salida-destino.json");
        var destino = destino("Ventas", campo("Fecha", "FECHA", null), campo("Cliente", "TEXTO", null),
                campo("Producto", "OPCION", List.of("Vela grande", "Vela chica", "Difusor")),
                campo("Cantidad", "NUMERO", null), campo("Importe", "NUMERO", null));
        var solicitud = new LecturaEvaluacion.SolicitudEvaluacion(List.of(
                new LecturaEvaluacion.CasoEvaluacion("con-destino", foto.toString(), "image/jpeg", destino)));
        Files.writeString(entrada, JsonMapper.builder().build().writeValueAsString(solicitud));

        EvaluacionIaApplication.main(new String[]{
                "--evaluacion.entrada=" + entrada, "--evaluacion.salida=" + salida,
                "--ensambla.ia.proveedor=simulado", "--spring.config.import=",
                "--spring.main.banner-mode=off", "--logging.level.root=WARN"});

        var resultado = JsonMapper.builder().build().readTree(Files.readString(salida)).get(0);
        assertThat(resultado.path("error").isNull()).isTrue();
        assertThat(resultado.path("camposNuevos")).hasSize(1);
        assertThat(resultado.path("camposNuevos").get(0).path("nombre").asString()).isEqualTo("¿Pagó?");
        assertThat(resultado.path("camposNuevos").get(0).path("tipo").asString()).isEqualTo("SI_NO");
    }

    @Test
    @DisplayName("Un resultado sin columnas nuevas informadas las deja vacías")
    void resultadoShouldDefaultToNoNewColumns() {
        var resultado = new LecturaEvaluacion.ResultadoEvaluacion("caso", null, "Falla", null);

        assertThat(resultado.camposNuevos()).isEmpty();
    }

    private tools.jackson.databind.JsonNode ejecutarConDestino(LectorCuadernoPort lector,
                                                               LecturaEvaluacion.DestinoEvaluacion destino)
            throws Exception {
        Path foto = carpeta.resolve("sintetico-destino.jpg");
        Files.write(foto, new byte[]{1, 2, 3});
        Path entrada = carpeta.resolve("entrada-mock.json");
        Path salida = carpeta.resolve("salida-mock.json");
        var solicitud = new LecturaEvaluacion.SolicitudEvaluacion(List.of(
                new LecturaEvaluacion.CasoEvaluacion("con-destino", foto.toString(), "image/jpeg", destino)));
        Files.writeString(entrada, JsonMapper.builder().build().writeValueAsString(solicitud));

        new LecturaEvaluacion(lector, JsonMapper.builder().build()).ejecutar(entrada, salida, Duration.ofSeconds(5));

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

    private LecturaCuaderno givenLectura(CampoPropuesto... campos) {
        Map<String, DatoLeido> datos = new java.util.LinkedHashMap<>();
        for (CampoPropuesto campo : campos) {
            datos.put(campo.clave(), new DatoLeido("1", 0.9));
        }
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, List.of(campos)),
                List.of(new FilaLeida(1, 1, datos)), List.of(), new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1));
    }

    private Path givenSolicitud() throws Exception {
        Path foto = carpeta.resolve("sintetico.jpg");
        Files.write(foto, new byte[]{1, 2, 3});
        Path entrada = carpeta.resolve("entrada.json");
        var solicitud = new LecturaEvaluacion.SolicitudEvaluacion(
                List.of(new LecturaEvaluacion.CasoEvaluacion("sintetico-001", foto.toString(), "image/jpeg")));
        Files.writeString(entrada, JsonMapper.builder().build().writeValueAsString(solicitud));
        return entrada;
    }
}
