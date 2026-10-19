package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.port.TallerIaPort;
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
import java.util.List;

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
        TallerIaPort port = config.tallerIaPort(config.promptTaller(), JsonMapper.builder().build(),
                propiedades(AVANZADO), new StaticListableBeanFactory().getBeanProvider(BedrockRuntimeClient.class));

        ResultadoTallerIa result = port.conversar(consultaDeTaller("ayuda"));

        assertThat(result.consumo().proveedor()).isEqualTo("simulado");
        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
    }

    @Test
    @DisplayName("Con Bedrock, el taller usa el modelo rápido, una sola llamada y las nueve herramientas")
    void tallerIaPortShouldUseFastModelWithAllTools() {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder().message(Message.builder()
                        .role(ConversationRole.ASSISTANT).content(ContentBlock.fromText("Hola")).build()).build())
                .usage(TokenUsage.builder().inputTokens(800).outputTokens(40).build())
                .build());

        ResultadoTallerIa result = config.tallerIaPort(config.promptTaller(), JsonMapper.builder().build(),
                propiedades(AVANZADO), proveedorDe(bedrock)).conversar(consultaDeTaller("hola"));

        assertThat(result.texto()).isEqualTo("Hola");
        assertThat(result.consumo().modelo()).isEqualTo(RAPIDO);
        org.mockito.ArgumentCaptor<ConverseRequest> captor = org.mockito.ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        assertThat(captor.getValue().inferenceConfig().maxTokens()).isEqualTo(400);
        assertThat(captor.getValue().toolConfig().tools())
                .extracting(herramienta -> herramienta.toolSpec().name())
                .containsExactly("agregar_pieza", "quitar_pieza", "renombrar_pieza", "renombrar_sistema",
                        "agregar_campo", "quitar_campo", "separar_en_seccion", "cambiar_distribucion",
                        "ofrecer_opciones");
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
