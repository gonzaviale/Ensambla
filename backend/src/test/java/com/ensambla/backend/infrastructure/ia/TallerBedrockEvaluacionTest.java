package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.service.sami.HerramientasTallerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluación del taller conversacional contra Amazon Bedrock de verdad: cuesta centavos. Se salta sola, salvo que se
 * defina {@code SAMI_BEDROCK_PERFIL}. Corre las conversaciones de {@code sami/conversaciones-taller.json} con el mismo
 * prompt, herramientas y validación que producción y mide el acierto de las acciones, los tokens y la latencia.
 * <pre>SAMI_BEDROCK_PERFIL=mi-perfil ./mvnw test -Dtest=TallerBedrockEvaluacionTest</pre>
 * Meta: 85 % de acierto y menos de 2000 tokens de entrada por turno.
 */
@EnabledIfEnvironmentVariable(named = "SAMI_BEDROCK_PERFIL", matches = ".+")
class TallerBedrockEvaluacionTest {

    private static final double META_ACIERTO = 0.85;
    /**
     * Piso real con Bedrock: ~350 tokens de sistema de herramientas del proveedor + los esquemas + el prompt + el
     * contexto. Con Haiku 4.5 son ≈ 0,2 centavos de dólar por turno.
     */
    private static final int META_TOKENS_DE_ENTRADA = 2000;
    private static final String MODELO = System.getenv().getOrDefault(
            "SAMI_BEDROCK_RAPIDO", "us.anthropic.claude-haiku-4-5-20251001-v1:0");
    private static final long PAUSA_ENTRE_CASOS_MS = Long.parseLong(
            System.getenv().getOrDefault("SAMI_BEDROCK_PAUSA_MS", "3000"));
    private static final long ESPERA_POR_LIMITE_MS = 15_000;
    private static final int REINTENTOS = 5;
    private static final java.util.concurrent.atomic.AtomicLong PAUSA_MS_DESCONTADA =
            new java.util.concurrent.atomic.AtomicLong();

    private record Esperada(String herramienta, Map<String, String> args) {
    }

    private record Caso(String id, String estado, String mensaje, List<MensajeTaller> historial,
                        List<Esperada> esperadas, Boolean opciones, Boolean permiteExtras, Boolean armado) {
    }

    @Test
    @DisplayName("Con Bedrock real, el taller acierta las acciones esperadas con pocos tokens")
    void tallerShouldMeetAccuracyAndTokenGoals() throws Exception {
        List<Caso> casos = casos();
        List<String> fallos = new ArrayList<>();
        int aciertos = 0;
        long tokensEntrada = 0;
        long tokensSalida = 0;
        long milisegundos = 0;
        try (BedrockRuntimeClient bedrock = BedrockRuntimeClient.builder()
                .region(Region.of(System.getenv().getOrDefault("SAMI_BEDROCK_REGION", "us-east-1")))
                .credentialsProvider(ProfileCredentialsProvider.create(System.getenv("SAMI_BEDROCK_PERFIL")))
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(60)))
                .build()) {
            JsonMapper mapper = JsonMapper.builder().build();
            DefinicionHerramienta[] herramientas = mapper.readValue(
                    recurso("/ia/herramientas-taller.json"), DefinicionHerramienta[].class);
            TallerIaAdapter adapter = new TallerIaAdapter(
                    new ClienteModeloConHerramientasBedrock(bedrock, MODELO, 900, List.of(herramientas)),
                    new PromptTaller(new ClassPathResource("ia/prompt-taller.md")));
            HerramientasTallerService validador = new HerramientasTallerService();
            for (Caso caso : casos) {
                EstadoTaller estado = estado(caso.estado());
                long inicio = System.nanoTime();
                ResultadoTallerIa resultado = conversar(adapter, new ConsultaTaller(
                        caso.mensaje(), estado, caso.historial() == null ? List.of() : caso.historial()));
                milisegundos += (System.nanoTime() - inicio) / 1_000_000 - PAUSA_MS_DESCONTADA.get();
                PAUSA_MS_DESCONTADA.set(0);
                Thread.sleep(PAUSA_ENTRE_CASOS_MS);
                tokensEntrada += resultado.consumo().tokensEntrada();
                tokensSalida += resultado.consumo().tokensSalida();
                ValidacionTaller validacion = validador.validar(resultado.llamadas(), estado);
                String problema = problema(caso, validacion, estado);
                if (problema == null) {
                    aciertos++;
                } else {
                    fallos.add("  - %s «%s»: %s | texto: %s".formatted(
                            caso.id(), caso.mensaje(), problema, resultado.texto().replace('\n', ' ')));
                }
            }
        }
        double acierto = (double) aciertos / casos.size();
        long promedioEntrada = tokensEntrada / casos.size();
        String reporte = "SAMI-TALLER %s: %d casos | acierto %d/%d = %.0f %% | tokens por turno %d entrada / %d salida"
                .formatted(MODELO, casos.size(), aciertos, casos.size(), acierto * 100, promedioEntrada,
                        tokensSalida / casos.size())
                + " | latencia media %d ms%n%s".formatted(milisegundos / casos.size(), String.join("\n", fallos));
        System.out.println(reporte);
        assertThat(acierto).as(reporte).isGreaterThanOrEqualTo(META_ACIERTO);
        assertThat(promedioEntrada).as(reporte).isLessThan(META_TOKENS_DE_ENTRADA);
    }

    /**
     * Las cuentas nuevas de Bedrock tienen una cuota baja de pedidos por minuto: ante un 429 se espera y se reintenta.
     * El tiempo esperado no cuenta para la latencia.
     */
    private ResultadoTallerIa conversar(TallerIaAdapter adapter, ConsultaTaller consulta) throws InterruptedException {
        for (int intento = 1; ; intento++) {
            try {
                return adapter.conversar(consulta);
            } catch (RuntimeException e) {
                boolean limitado = String.valueOf(e.getMessage()).contains("Too many requests");
                if (!limitado || intento == REINTENTOS) {
                    throw e;
                }
                Thread.sleep(ESPERA_POR_LIMITE_MS);
                PAUSA_MS_DESCONTADA.addAndGet(ESPERA_POR_LIMITE_MS);
            }
        }
    }

    /** {@code null} si el caso se cumplió; si no, qué falló. */
    private String problema(Caso caso, ValidacionTaller validacion, EstadoTaller estado) {
        List<AccionTaller> acciones = validacion.acciones();
        for (Esperada esperada : caso.esperadas()) {
            if (acciones.stream().noneMatch(accion -> cumple(accion, esperada, estado))) {
                return "falta %s %s, vino %s".formatted(esperada.herramienta(), esperada.args(), acciones);
            }
        }
        if (!Boolean.TRUE.equals(caso.permiteExtras()) && acciones.size() > caso.esperadas().size()) {
            return "acciones de más: " + acciones;
        }
        if (Boolean.TRUE.equals(caso.opciones()) && validacion.opciones() == null) {
            return "tenía que ofrecer opciones";
        }
        return null;
    }

    private boolean cumple(AccionTaller accion, Esperada esperada, EstadoTaller estado) {
        if (!accion.herramienta().equals(esperada.herramienta())) {
            return false;
        }
        return esperada.args().entrySet().stream().allMatch(arg -> {
            Object real = accion.argumentos().get(arg.getKey());
            if (arg.getKey().equals("pieza")) {
                return tipoDe(String.valueOf(real), estado).equals(tipoDe(arg.getValue(), estado));
            }
            return real != null && normalizar(String.valueOf(real)).contains(normalizar(arg.getValue()));
        });
    }

    private String tipoDe(String referencia, EstadoTaller estado) {
        return estado.piezas().stream()
                .filter(pieza -> referencia.equals(pieza.id()))
                .map(PiezaTaller::tipo)
                .findFirst()
                .orElse(referencia);
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private List<Caso> casos() throws IOException {
        try (InputStream json = recurso("/sami/conversaciones-taller.json")) {
            return JsonMapper.builder().build().readValue(json, new TypeReference<>() { });
        }
    }

    private InputStream recurso(String ruta) {
        return getClass().getResourceAsStream(ruta);
    }

    private EstadoTaller estado(String nombre) {
        PiezaTaller inventario = new PiezaTaller("e1", "inventory", "Inventario", null, List.of(
                new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                new CampoTaller("Precio", "currency")));
        PiezaTaller ventas = new PiezaTaller("e2", "sales", "Ventas", null, List.of(
                new CampoTaller("Producto", "text"), new CampoTaller("Cantidad", "number"),
                new CampoTaller("Total", "currency")));
        PiezaTaller clientes = new PiezaTaller("e3", "clients", "Clientes", null, List.of(
                new CampoTaller("Nombre", "text"), new CampoTaller("Teléfono", "tel")));
        PiezaTaller gastos = new PiezaTaller("e4", "expenses", "Gastos", null, List.of(
                new CampoTaller("Detalle", "text"), new CampoTaller("Monto", "currency")));
        return switch (nombre) {
            case "vacio" -> new EstadoTaller("Mi sistema", "mosaic", List.of());
            case "completo" -> new EstadoTaller("Mi almacén", "mosaic", List.of(inventario, ventas, clientes, gastos));
            default -> new EstadoTaller("Mi almacén", "mosaic", List.of(inventario, ventas, clientes));
        };
    }
}
