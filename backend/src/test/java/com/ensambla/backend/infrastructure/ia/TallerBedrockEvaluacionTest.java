package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.FilaTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.service.sami.HerramientasTallerService;
import com.ensambla.backend.domain.service.sami.RubroTallerService;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluación del taller conversacional contra Amazon Bedrock de verdad: cuesta centavos. Se salta sola, salvo que se
 * defina {@code SAMI_BEDROCK_PERFIL}. Corre las conversaciones de {@code sami/conversaciones-taller.json} con el mismo
 * prompt, catálogo, kits, herramientas y validación que producción y mide el acierto de las acciones, los tokens y la
 * latencia.
 * <pre>SAMI_BEDROCK_PERFIL=mi-perfil ./mvnw test -Dtest=TallerBedrockEvaluacionTest</pre>
 * Meta: 85 % de acierto y menos de 3200 tokens de entrada por turno (sin foto). Con
 * {@code SAMI_EVAL_CASOS=foto} corren sólo los casos cuyo id contiene «foto».
 * <p>
 * Cada caso puede pedir: {@code esperadas} (todas tienen que estar), {@code alguna} (alcanza con una), {@code opciones}
 * (tiene que haber botonera), {@code permiteExtras} (puede hacer más de lo esperado) y {@code armado} (arma al menos
 * dos piezas o usa un kit). {@code usar_kit} se busca en lo que pidió el modelo, porque la validación lo expande;
 * {@code pieza}, {@code desde} y {@code hacia} se comparan por tipo (o por nombre, si es una tabla a medida). El resto
 * de los argumentos se compara como texto contenido, sin mayúsculas ni tildes: un valor vacío sólo pide que el
 * argumento venga (la {@code posicion} de un {@code editar_campo}, por ejemplo, que puede ser 1 o 2 según el orden en
 * que el modelo mueva los datos).
 * <p>
 * Un caso con {@code imagen} adjunta ese archivo de {@code src/test/resources/sami/} (por ejemplo
 * {@code cuaderno-ventas-velas.png}); sus tokens de entrada se informan aparte y no cuentan para la meta de tokens.
 * <p>
 * En el estado {@code almacen} las ventas ya tienen el vínculo {@code Cliente}, como las arma el taller
 * ({@code wireSystem}), para poder evaluar {@code editar_campo} sobre ese dato.
 */
@EnabledIfEnvironmentVariable(named = "SAMI_BEDROCK_PERFIL", matches = ".+")
class TallerBedrockEvaluacionTest {

    private static final double META_ACIERTO = 0.85;
    /**
     * Piso real con Bedrock: ~350 tokens de sistema de herramientas del proveedor + los esquemas de las 13 herramientas
     * (cerca de la mitad del turno) + el prompt + el contexto. Con 2000 había que sacar reglas del prompt que llevaron
     * el acierto de 80 % a 100 %. Con editar_campo (13 herramientas) la meta pasó de 2300 a 2600: v2, 2026-10-20,
     * 41 casos, 2542 de entrada y 162 de salida, ≈ 0,33 centavos de dólar por turno
     * con Haiku 4.5. v3 suma cargar_registros (14 herramientas) y la meta pasa a 2800: 2026-10-20, 45 casos, 2682 de
     * entrada sin foto y ≈ 3500 con foto (≈ 0,35 centavos por turno con foto).
     * v5 suma editar_registro y quitar_registro (16 herramientas) y las primeras filas de cada pieza en el contexto, como
     * en la app: 2026-10-25, 53 casos, 3063 de entrada sin foto y ≈ 3980 con foto; la meta pasa a 3200.
     */
    private static final int META_TOKENS_DE_ENTRADA = 3200;
    private static final String MODELO = System.getenv().getOrDefault(
            "SAMI_BEDROCK_RAPIDO", "us.anthropic.claude-haiku-4-5-20251001-v1:0");
    private static final long PAUSA_ENTRE_CASOS_MS = Long.parseLong(
            System.getenv().getOrDefault("SAMI_BEDROCK_PAUSA_MS", "3000"));
    private static final long ESPERA_POR_LIMITE_MS = 15_000;
    private static final int REINTENTOS = 5;
    private static final AtomicLong PAUSA_MS_DESCONTADA = new AtomicLong();
    private static final Set<String> REFERENCIAS_A_PIEZAS = Set.of("pieza", "desde", "hacia");

    private record Esperada(String herramienta, Map<String, String> args) {
    }

    /** @param imagen archivo de {@code src/test/resources/sami/} que la persona adjunta en el turno; nulo si no hay */
    private record Caso(String id, String estado, String mensaje, List<MensajeTaller> historial,
                        List<Esperada> esperadas, List<Esperada> alguna, Boolean opciones, Boolean permiteExtras,
                        Boolean armado, String imagen) {
    }

    @Test
    @DisplayName("Con Bedrock real, el taller acierta las acciones esperadas con pocos tokens")
    void tallerShouldMeetAccuracyAndTokenGoals() throws Exception {
        List<Caso> casos = casos();
        List<String> fallos = new ArrayList<>();
        int aciertos = 0;
        long tokensEntrada = 0;
        long tokensSalida = 0;
        long tokensEntradaConFoto = 0;
        int conFoto = 0;
        long milisegundos = 0;
        CatalogoComponentes catalogo = SamiPruebas.catalogoReal();
        KitsRubro kits = SamiPruebas.kitsReales();
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
                    new PromptTaller(new ClassPathResource("ia/prompt-taller.md"), catalogo, kits));
            HerramientasTallerService validador = new HerramientasTallerService(catalogo, kits);
            RubroTallerService rubros = new RubroTallerService(kits);
            for (Caso caso : casos) {
                EstadoTaller estado = estado(caso.estado());
                List<MensajeTaller> historial = Objects.requireNonNullElse(caso.historial(), List.of());
                KitRubro kit = rubros.detectar(caso.mensaje(), estado, historial).orElse(null);
                long inicio = System.nanoTime();
                ImagenTaller imagen = imagen(caso.imagen());
                ResultadoTallerIa resultado = conversar(adapter, new ConsultaTaller(
                        caso.mensaje(), estado, historial, kit, imagen));
                milisegundos += (System.nanoTime() - inicio) / 1_000_000 - PAUSA_MS_DESCONTADA.get();
                PAUSA_MS_DESCONTADA.set(0);
                Thread.sleep(PAUSA_ENTRE_CASOS_MS);
                // Una foto suma del orden de mil tokens: va aparte para que no tape la meta de los turnos de texto.
                if (imagen == null) {
                    tokensEntrada += resultado.consumo().tokensEntrada();
                } else {
                    tokensEntradaConFoto += resultado.consumo().tokensEntrada();
                    conFoto++;
                }
                tokensSalida += resultado.consumo().tokensSalida();
                ValidacionTaller validacion = validador.validar(resultado.llamadas(), estado);
                String problema = problema(caso, validacion, resultado.llamadas(), estado, kits);
                if (problema == null) {
                    aciertos++;
                } else {
                    // Las llamadas crudas (antes de validar) muestran si el modelo no la pidió o la validación la descartó.
                    fallos.add("  - %s «%s»: %s | texto: %s | crudas: %s | descartadas: %d".formatted(
                            caso.id(), caso.mensaje(), problema, resultado.texto().replace('\n', ' '),
                            resultado.llamadas(), validacion.descartadas()));
                }
            }
        }
        double acierto = (double) aciertos / casos.size();
        int sinFoto = casos.size() - conFoto;
        long promedioEntrada = sinFoto == 0 ? 0 : tokensEntrada / sinFoto;
        String reporte = "SAMI-TALLER %s %s: %d casos | acierto %d/%d = %.0f %% | tokens por turno %d entrada / %d salida"
                .formatted(MODELO, PromptTaller.VERSION, casos.size(), aciertos, casos.size(), acierto * 100,
                        promedioEntrada, tokensSalida / casos.size())
                + " | con foto %d entrada".formatted(conFoto == 0 ? 0 : tokensEntradaConFoto / conFoto)
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
    private String problema(Caso caso, ValidacionTaller validacion, List<LlamadaHerramienta> llamadas,
                            EstadoTaller estado, KitsRubro kits) {
        List<AccionTaller> acciones = validacion.acciones();
        for (Esperada esperada : caso.esperadas()) {
            if (!aparece(esperada, acciones, llamadas, estado)) {
                return "falta %s %s, vino %s".formatted(esperada.herramienta(), esperada.args(), acciones);
            }
        }
        List<Esperada> alguna = Objects.requireNonNullElse(caso.alguna(), List.of());
        if (!alguna.isEmpty() && alguna.stream().noneMatch(esperada -> aparece(esperada, acciones, llamadas, estado))) {
            return "no vino ninguna de %s, vino %s".formatted(alguna, acciones);
        }
        if (!Boolean.TRUE.equals(caso.permiteExtras()) && acciones.size() > caso.esperadas().size()) {
            return "acciones de más: " + acciones;
        }
        if (Boolean.TRUE.equals(caso.opciones()) && validacion.opciones() == null) {
            return "tenía que ofrecer opciones";
        }
        if (Boolean.TRUE.equals(caso.armado()) && !armo(acciones, llamadas, kits)) {
            return "tenía que armar (dos piezas o un kit), vino " + acciones;
        }
        return null;
    }

    private boolean aparece(Esperada esperada, List<AccionTaller> acciones, List<LlamadaHerramienta> llamadas,
                            EstadoTaller estado) {
        if (esperada.herramienta().equals("usar_kit")) {
            return llamadas.stream()
                    .filter(llamada -> llamada.nombre().equals("usar_kit"))
                    .anyMatch(llamada -> cumple(llamada.argumentos(), esperada, estado));
        }
        return acciones.stream()
                .filter(accion -> accion.herramienta().equals(esperada.herramienta()))
                .anyMatch(accion -> cumple(accion.argumentos(), esperada, estado));
    }

    private boolean armo(List<AccionTaller> acciones, List<LlamadaHerramienta> llamadas, KitsRubro kits) {
        boolean usoUnKit = llamadas.stream()
                .filter(llamada -> llamada.nombre().equals("usar_kit"))
                .anyMatch(llamada -> kits.buscar(String.valueOf(llamada.argumentos().get("rubro"))).isPresent());
        long piezas = acciones.stream().filter(accion -> accion.herramienta().equals("agregar_pieza")).count();
        return usoUnKit || piezas >= 2;
    }

    private boolean cumple(Map<String, Object> argumentos, Esperada esperada, EstadoTaller estado) {
        return esperada.args().entrySet().stream().allMatch(arg -> {
            Object real = argumentos.get(arg.getKey());
            if (real == null) {
                return false;
            }
            String texto = normalizar(String.valueOf(real));
            String buscado = normalizar(arg.getValue());
            if (REFERENCIAS_A_PIEZAS.contains(arg.getKey())) {
                return tipoDe(String.valueOf(real), estado).equals(tipoDe(arg.getValue(), estado))
                        || texto.contains(buscado);
            }
            return texto.contains(buscado);
        });
    }

    /** El tipo de la pieza a la que apunta una referencia (por id o por nombre); si no la encuentra, la referencia. */
    private String tipoDe(String referencia, EstadoTaller estado) {
        return estado.piezas().stream()
                .filter(pieza -> referencia.equals(pieza.id()) || referencia.equalsIgnoreCase(pieza.nombre()))
                .map(PiezaTaller::tipo)
                .findFirst()
                .orElse(referencia.toLowerCase(Locale.ROOT));
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Las conversaciones del set. Con {@code SAMI_EVAL_CASOS} (ids separados por coma, o un pedazo del id) corre sólo
     * esas: sirve para probar un ajuste del prompt sin pagar el set entero. La meta se mide siempre con el set completo.
     */
    private List<Caso> casos() throws IOException {
        List<Caso> todos;
        try (InputStream json = recurso("/sami/conversaciones-taller.json")) {
            todos = JsonMapper.builder().build().readValue(json, new TypeReference<>() { });
        }
        String filtro = System.getenv("SAMI_EVAL_CASOS");
        if (filtro == null || filtro.isBlank()) {
            return todos;
        }
        List<String> partes = List.of(filtro.split(","));
        return todos.stream().filter(caso -> partes.stream().anyMatch(parte -> caso.id().contains(parte.strip())))
                .toList();
    }

    /** La foto del caso, con el tipo según la extensión; nula si el caso no adjunta ninguna. */
    private ImagenTaller imagen(String archivo) throws IOException {
        if (archivo == null) {
            return null;
        }
        try (InputStream datos = recurso("/sami/" + archivo)) {
            String extension = archivo.substring(archivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            String tipo = "image/" + (extension.equals("jpg") ? "jpeg" : extension);
            return new ImagenTaller(tipo, Objects.requireNonNull(datos, archivo).readAllBytes());
        }
    }

    private InputStream recurso(String ruta) {
        return getClass().getResourceAsStream(ruta);
    }

    private EstadoTaller estado(String nombre) {
        List<CampoTaller> camposDeInventario = List.of(new CampoTaller("Producto", "text"),
                new CampoTaller("Cantidad", "number"), new CampoTaller("Precio", "currency"));
        // Sami ve las primeras filas de cada pieza, como en la app: así el contexto pesa lo que pesa de verdad.
        PiezaTaller inventario = new PiezaTaller("e1", "inventory", "Inventario", null, camposDeInventario, List.of(
                new FilaTaller("f11", List.of("Yerba", "24", "3600")),
                new FilaTaller("f12", List.of("Fideos", "30", "1500")),
                new FilaTaller("f13", List.of("Arroz", "5", "1000"))));
        PiezaTaller inventarioConArrozRepetido = new PiezaTaller("e1", "inventory", "Inventario", null,
                camposDeInventario, List.of(
                new FilaTaller("f31", List.of("Arroz", "5", "1000")),
                new FilaTaller("f32", List.of("Arroz", "8", "1100")),
                new FilaTaller("f33", List.of("Yerba", "24", "3600"))));
        PiezaTaller ventas = new PiezaTaller("e2", "sales", "Ventas", null, List.of(
                new CampoTaller("Producto", "relation"), new CampoTaller("Cantidad", "number"),
                new CampoTaller("Precio", "currency"), new CampoTaller("Cliente", "relation")));
        PiezaTaller clientes = new PiezaTaller("e3", "clients", "Clientes", null, List.of(
                new CampoTaller("Nombre", "text"), new CampoTaller("Teléfono", "tel")), List.of(
                new FilaTaller("f21", List.of("Lucía", "11 5555-0101")),
                new FilaTaller("f22", List.of("Marta", "11 5555-0102"))));
        PiezaTaller gastos = new PiezaTaller("e4", "expenses", "Gastos", null, List.of(
                new CampoTaller("Concepto", "text"), new CampoTaller("Importe", "currency")));
        PiezaTaller turnos = new PiezaTaller("e5", "form", "Turnos", null, List.of(
                new CampoTaller("Nombre", "text"), new CampoTaller("Fecha", "date"), new CampoTaller("Hora", "time")));
        return switch (nombre) {
            case "vacio" -> new EstadoTaller("Mi sistema", "mosaic", List.of());
            case "completo" -> new EstadoTaller("Mi almacén", "mosaic", List.of(inventario, ventas, clientes, gastos));
            case "repetidos" -> new EstadoTaller("Mi almacén", "mosaic",
                    List.of(inventarioConArrozRepetido, ventas, clientes));
            case "peluqueria" -> new EstadoTaller("Mi peluquería", "mosaic", List.of(turnos, clientes));
            default -> new EstadoTaller("Mi almacén", "mosaic", List.of(inventario, ventas, clientes));
        };
    }
}
