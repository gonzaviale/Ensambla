package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.service.sami.HerramientasTallerService;
import com.ensambla.backend.domain.service.sami.RubroTallerService;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SamiConfigTest {

    private static final String RAPIDO = "modelo-rapido";
    private static final String AVANZADO = "modelo-avanzado";

    private final SamiConfig config = new SamiConfig();
    private ConsultaIaSami consulta;

    @BeforeEach
    void setUp() {
        consulta = new ConsultaIaSami("vendo velas", List.of(), SamiPruebas.plantillas());
    }

    @Test
    @DisplayName("Carga el diccionario del recurso con plantillas, términos, columnas y preguntas")
    void diccionarioSamiShouldLoadBundledDictionary() {
        DiccionarioSami diccionario = config.diccionarioSami(JsonMapper.builder().build());

        assertThat(diccionario.plantillas()).extracting("plantilla")
                .containsExactly("Ventas", "Inventario", "Pedidos", "Clientes", "Gastos");
        assertThat(diccionario.plantillas()).allSatisfy(plantilla -> assertThat(plantilla.terminos()).isNotEmpty());
        assertThat(diccionario.columnas()).extracting("nombre").contains("Saldo pendiente", "Forma de pago");
        assertThat(diccionario.preguntas()).extracting("id").contains("principal", "venta-o-stock");
    }

    @Test
    @DisplayName("Todas las plantillas y columnas del diccionario existen en la base y las preguntas apuntan a ellas")
    void diccionarioSamiShouldOnlyReferenceKnownTemplates() {
        DiccionarioSami diccionario = config.diccionarioSami(JsonMapper.builder().build());
        List<String> nombres = diccionario.plantillas().stream().map(p -> p.plantilla()).toList();

        assertThat(diccionario.columnas()).allSatisfy(columna -> assertThat(nombres).containsAll(columna.plantillas()));
        assertThat(diccionario.preguntas()).allSatisfy(pregunta -> assertThat(pregunta.opciones())
                .allSatisfy(opcion -> assertThat(opcion.plantilla() == null || nombres.contains(opcion.plantilla()))
                        .isTrue()));
    }

    @Test
    @DisplayName("Sin Bedrock, la IA es el cliente simulado: elige la primera plantilla sin costo")
    void samiIaPortShouldUseSimulatedClientWithoutBedrock() {
        SamiIaPort port = config.samiIaPort(config.promptSami(), JsonMapper.builder().build(), propiedades(AVANZADO),
                new StaticListableBeanFactory().getBeanProvider(BedrockRuntimeClient.class));

        PropuestaIaSami result = port.proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.consumo().proveedor()).isEqualTo("simulado");
    }

    @Test
    @DisplayName("Con Bedrock usa el modelo rápido y escala al avanzado cuando el primero no está seguro")
    void samiIaPortShouldEscalateFromFastToAdvancedModel() {
        BedrockRuntimeClient bedrock = bedrockQueResponde("{\"plantilla\":\"Ventas\",\"confianza\":0.2,\"columnas\":[]}");

        PropuestaIaSami result = config.samiIaPort(config.promptSami(), JsonMapper.builder().build(),
                propiedades(AVANZADO), proveedorDe(bedrock)).proponer(consulta);

        assertThat(result.consumo().modelo()).isEqualTo(RAPIDO + " + " + AVANZADO);
        assertThat(modelosConsultados(bedrock, 2)).containsExactly(RAPIDO, AVANZADO);
    }

    @Test
    @DisplayName("Sin modelo avanzado configurado nunca escala")
    void samiIaPortShouldNotEscalateWithoutAdvancedModel() {
        BedrockRuntimeClient bedrock = bedrockQueResponde("{\"plantilla\":\"Ventas\",\"confianza\":0.2,\"columnas\":[]}");

        for (String sinAvanzado : new String[]{null, "  "}) {
            config.samiIaPort(config.promptSami(), JsonMapper.builder().build(), propiedades(sinAvanzado),
                    proveedorDe(bedrock)).proponer(consulta);
        }

        assertThat(modelosConsultados(bedrock, 2)).containsExactly(RAPIDO, RAPIDO);
    }

    @Test
    @DisplayName("Sin Bedrock, la IA del taller es el cliente simulado: sin costo y con botonera")
    void tallerIaPortShouldUseSimulatedClientWithoutBedrock() {
        TallerIaPort port = config.tallerIaPort(promptTaller(), JsonMapper.builder().build(), propiedades(AVANZADO),
                new StaticListableBeanFactory().getBeanProvider(BedrockRuntimeClient.class), kits());

        ResultadoTallerIa result = port.conversar(consultaDeTaller("ayuda"));

        assertThat(result.consumo().proveedor()).isEqualTo("simulado");
        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
    }

    @Test
    @DisplayName("Carga el catálogo de componentes generado por el frontend con sus diez tipos")
    void catalogoComponentesShouldLoadBundledCatalog() {
        CatalogoComponentes catalogo = config.catalogoComponentes(JsonMapper.builder().build());

        assertThat(catalogo.componentes()).extracting(ComponenteCatalogo::tipo).containsExactly(
                "inventory", "orders", "clients", "sales", "expenses", "summary", "form", "note", "entries", "exits");
        assertThat(catalogo.componentes()).allSatisfy(componente -> assertThat(componente.singular()).isNotBlank());
    }

    @Test
    @DisplayName("Carga los diez kits por rubro con ids únicos y términos en minúscula y sin tildes")
    void kitsRubroShouldLoadTenKitsWithUniqueIds() {
        KitsRubro kits = kits();

        assertThat(kits.kits()).extracting(KitRubro::id).containsExactly("almacen", "dietetica", "velas",
                "panaderia", "viandas", "ropa", "peluqueria", "taller-mecanico", "ferreteria", "libreria");
        assertThat(kits.kits()).allSatisfy(kit -> assertThat(kit.terminos())
                .isNotEmpty()
                .allMatch(termino -> termino.matches("[a-z0-9 ]+")));
    }

    @Test
    @DisplayName("Cada pieza de un kit usa un componente del catálogo, con datos válidos y sin repetir tipos")
    void kitsRubroShouldOnlyUseCatalogComponentsAndValidFields() {
        CatalogoComponentes catalogo = config.catalogoComponentes(JsonMapper.builder().build());

        for (KitRubro kit : kits().kits()) {
            List<String> tipos = kit.piezas().stream().map(PiezaKit::tipo).toList();
            assertThat(tipos).as("tipos de %s", kit.id()).allMatch(tipo -> catalogo.buscar(tipo).isPresent());
            assertThat(tipos.stream().filter(tipo -> !tipo.equals(CatalogoComponentes.TIPO_A_MEDIDA)))
                    .as("tipos que no se repiten en %s", kit.id()).doesNotHaveDuplicates();
            assertThat(kit.piezas()).extracting(PiezaKit::nombre).as("nombres de %s", kit.id())
                    .doesNotHaveDuplicates()
                    .allMatch(nombre -> nombre != null && !nombre.isBlank());
            for (PiezaKit pieza : kit.piezas()) {
                List<String> etiquetas = etiquetas(catalogo, pieza);
                assertThat(etiquetas).as("datos de %s/%s", kit.id(), pieza.nombre()).doesNotHaveDuplicates()
                        .hasSizeLessThanOrEqualTo(CatalogoTaller.MAX_CAMPOS_POR_PIEZA);
                assertThat(pieza.campos()).as("datos extra de %s/%s", kit.id(), pieza.nombre())
                        .hasSizeLessThanOrEqualTo(CatalogoTaller.MAX_CAMPOS_EXTRA)
                        .allSatisfy(campo -> assertThat(CatalogoTaller.TIPOS_DE_CAMPO).contains(campo.tipo()))
                        .allSatisfy(campo -> assertThat(campo.opciones().size()).isBetween(
                                campo.tipo().equals("select") ? 2 : 0, campo.tipo().equals("select") ? 6 : 0));
                assertThat(pieza.ejemplos()).as("ejemplos de %s/%s", kit.id(), pieza.nombre())
                        .hasSizeBetween(2, 3)
                        .allSatisfy(fila -> assertThat(etiquetas).containsAll(fila.keySet()))
                        .allSatisfy(fila -> assertThat(fila.values()).allMatch(this::esValorDeEjemplo));
            }
        }
    }

    @Test
    @DisplayName("Las conexiones de cada kit unen dos piezas distintas del mismo kit y tienen etiqueta")
    void kitsRubroShouldOnlyConnectPiecesOfTheKit() {
        for (KitRubro kit : kits().kits()) {
            List<String> nombres = kit.piezas().stream().map(PiezaKit::nombre).toList();
            assertThat(kit.conexiones()).as("conexiones de %s", kit.id()).allSatisfy(conexion -> {
                assertThat(nombres).contains(conexion.desde(), conexion.hacia());
                assertThat(conexion.desde()).isNotEqualTo(conexion.hacia());
                assertThat(conexion.etiqueta()).isNotBlank();
            });
            assertThat(kit.pregunta().pregunta()).as("pregunta de %s", kit.id()).isNotBlank();
            assertThat(kit.pregunta().opciones()).as("opciones de %s", kit.id()).hasSizeBetween(2, 4);
        }
    }

    @Test
    @DisplayName("Cada kit se arma entero, sin descartes, sobre un sistema vacío")
    void kitsRubroShouldExpandWithoutDiscards() {
        HerramientasTallerService herramientas =
                new HerramientasTallerService(config.catalogoComponentes(JsonMapper.builder().build()), kits());

        for (KitRubro kit : kits().kits()) {
            ValidacionTaller result = herramientas.validar(
                    List.of(new LlamadaHerramienta("usar_kit", Map.of("rubro", kit.id()))),
                    new EstadoTaller("Mi sistema", "mosaic", List.of()));

            assertThat(result.acciones()).as("acciones de %s", kit.id())
                    .hasSize(kit.piezas().size() + kit.conexiones().size());
            List<Integer> ejemplos = result.acciones().stream()
                    .filter(accion -> accion.herramienta().equals("agregar_pieza"))
                    .map(accion -> ((List<?>) accion.argumentos().get("ejemplos")).size())
                    .toList();
            assertThat(ejemplos).as("ejemplos de %s", kit.id())
                    .isEqualTo(kit.piezas().stream().map(pieza -> pieza.ejemplos().size()).toList());
        }
    }

    @Test
    @DisplayName("Cada kit se reconoce por su propio nombre, que es lo que ofrece la botonera")
    void kitsRubroShouldBeDetectedByTheirOwnName() {
        RubroTallerService rubros = new RubroTallerService(kits());

        for (KitRubro kit : kits().kits()) {
            assertThat(rubros.detectar(kit.nombre(), new EstadoTaller(null, "mosaic", List.of()), List.of()))
                    .as("rubro de «%s»", kit.nombre())
                    .map(KitRubro::id)
                    .contains(kit.id());
        }
    }

    @Test
    @DisplayName("Con Bedrock, el taller usa el modelo rápido, una sola llamada y las doce herramientas")
    void tallerIaPortShouldUseFastModelWithAllTools() {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(ContentBlock.fromText("Hola")).build()).build())
                .usage(TokenUsage.builder().inputTokens(800).outputTokens(40).build())
                .build());

        ResultadoTallerIa result = config.tallerIaPort(promptTaller(), JsonMapper.builder().build(),
                propiedades(AVANZADO), proveedorDe(bedrock), kits()).conversar(consultaDeTaller("hola"));

        assertThat(result.texto()).isEqualTo("Hola");
        assertThat(result.consumo().modelo()).isEqualTo(RAPIDO);
        org.mockito.ArgumentCaptor<ConverseRequest> captor = org.mockito.ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        assertThat(captor.getValue().inferenceConfig().maxTokens()).isEqualTo(400);
        assertThat(captor.getValue().toolConfig().tools())
                .extracting(herramienta -> herramienta.toolSpec().name())
                .containsExactly("agregar_pieza", "usar_kit", "quitar_pieza", "renombrar_pieza", "renombrar_sistema",
                        "agregar_campo", "quitar_campo", "cargar_ejemplos", "conectar", "separar_en_seccion",
                        "cambiar_distribucion", "ofrecer_opciones");
    }

    @Test
    @DisplayName("Las herramientas del taller del recurso coinciden con las que valida el dominio")
    void bundledToolsShouldMatchTheDomainTools() throws Exception {
        DefinicionHerramienta[] definiciones = JsonMapper.builder().build().readValue(
                getClass().getResourceAsStream("/ia/herramientas-taller.json"), DefinicionHerramienta[].class);

        assertThat(definiciones).extracting(DefinicionHerramienta::name).containsExactlyInAnyOrder(
                java.util.Arrays.stream(com.ensambla.backend.domain.model.sami.taller.HerramientaTaller.values())
                        .map(com.ensambla.backend.domain.model.sami.taller.HerramientaTaller::nombre)
                        .toArray(String[]::new));
        assertThat(definiciones).allSatisfy(definicion -> assertThat(definicion.inputSchema()).containsKey("type"));
    }

    @Test
    @DisplayName("Crea el cliente de Bedrock con el perfil de AWS indicado o con las credenciales por defecto")
    void bedrockRuntimeClientShouldBuildWithProfileOrDefaultCredentials() {
        try (BedrockRuntimeClient conPerfil = config.bedrockRuntimeClient(propiedades("x", "perfil-de-prueba"));
             BedrockRuntimeClient sinPerfil = config.bedrockRuntimeClient(propiedades("x", null));
             BedrockRuntimeClient perfilEnBlanco = config.bedrockRuntimeClient(propiedades("x", "  "))) {
            assertThat(conPerfil).isNotNull();
            assertThat(sinPerfil).isNotNull();
            assertThat(perfilEnBlanco).isNotNull();
        }
    }

    private ConsultaTaller consultaDeTaller(String mensaje) {
        return new ConsultaTaller(mensaje, new EstadoTaller("Mi almacén", "mosaic", List.of()), List.of());
    }

    private KitsRubro kits() {
        return config.kitsRubro(JsonMapper.builder().build());
    }

    private PromptTaller promptTaller() {
        return config.promptTaller(config.catalogoComponentes(JsonMapper.builder().build()), kits());
    }

    /** Las etiquetas de una pieza de kit: las de su componente más las extra. */
    private List<String> etiquetas(CatalogoComponentes catalogo, PiezaKit pieza) {
        List<String> etiquetas = new ArrayList<>();
        catalogo.buscar(pieza.tipo()).orElseThrow().campos().forEach(campo -> etiquetas.add(campo.etiqueta()));
        pieza.campos().forEach(campo -> etiquetas.add(campo.etiqueta()));
        return etiquetas;
    }

    private boolean esValorDeEjemplo(Object valor) {
        return valor instanceof Number
                || valor instanceof Boolean
                || valor instanceof String texto && !texto.isBlank() && texto.length() <= CatalogoTaller.MAX_LARGO_VALOR;
    }

    private SamiProperties propiedades(String modeloAvanzado) {
        return propiedades(modeloAvanzado, null);
    }

    private SamiProperties propiedades(String modeloAvanzado, String perfilAws) {
        return new SamiProperties(0.7, 3, 20, 60, new SamiProperties.Ia(
                "bedrock", "us-east-1", perfilAws, RAPIDO, modeloAvanzado, Duration.ofSeconds(30), 700, 0.6, 400));
    }

    private ObjectProvider<BedrockRuntimeClient> proveedorDe(BedrockRuntimeClient bedrock) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("bedrockRuntimeClient", bedrock);
        return factory.getBeanProvider(BedrockRuntimeClient.class);
    }

    private BedrockRuntimeClient bedrockQueResponde(String json) {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(ContentBlock.fromText(json)).build()).build())
                .usage(TokenUsage.builder().inputTokens(10).outputTokens(5).build())
                .build());
        return bedrock;
    }

    private List<String> modelosConsultados(BedrockRuntimeClient bedrock, int veces) {
        org.mockito.ArgumentCaptor<ConverseRequest> captor = org.mockito.ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock, times(veces)).converse(captor.capture());
        return captor.getAllValues().stream().map(ConverseRequest::modelId).toList();
    }
}
