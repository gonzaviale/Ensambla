package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.infrastructure.ia.RespuestasAnthropicDePrueba.anthropicQueResponde;
import static com.ensambla.backend.infrastructure.ia.RespuestasDePrueba.bedrockQueResponde;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SamiConfigTest {

    private static final String RAPIDO = "modelo-rapido";
    private static final String AVANZADO = "modelo-avanzado";
    private static final String CLAVE = "clave-de-prueba";
    private static final String DUDOSA = "{\"plantilla\":\"Ventas\",\"confianza\":0.2,\"columnas\":[]}";
    private static final String[] HERRAMIENTAS = {"agregar_pieza", "usar_kit", "quitar_pieza", "renombrar_pieza",
            "renombrar_sistema", "agregar_campo", "quitar_campo", "editar_campo", "cargar_registros",
            "editar_registros", "borrar_registros", "conectar", "separar_en_seccion", "cambiar_distribucion",
            "ofrecer_opciones"};

    private final SamiConfig config = new SamiConfig();
    private final ConsultaIaSami consulta = new ConsultaIaSami("vendo velas", List.of(), SamiPruebas.plantillas());

    private BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
    private AnthropicClient anthropic = mock(AnthropicClient.class);
    private DiccionarioSami diccionario;
    private PropuestaIaSami propuesta;
    private ResultadoTallerIa resultadoDelTaller;
    private CatalogoComponentes catalogo;
    private KitsRubro kits;
    private DefinicionHerramienta[] definiciones;

    @Test
    @DisplayName("Carga el diccionario del recurso con plantillas, términos, columnas y preguntas")
    void cargaElDiccionarioDelRecursoConPlantillasTerminosColumnasYPreguntas() {
        cuandoSeCargaElDiccionario();

        entoncesElDiccionarioTienePlantillasTerminosColumnasYPreguntas();
    }

    @Test
    @DisplayName("Todas las plantillas y columnas del diccionario existen en la base y las preguntas apuntan a ellas")
    void todasLasPlantillasYColumnasDelDiccionarioExistenEnLaBaseYLasPreguntasApuntanAEllas() {
        cuandoSeCargaElDiccionario();

        entoncesElDiccionarioSoloReferenciaPlantillasConocidas();
    }

    @Test
    @DisplayName("Con el proveedor global simulado y sin pisarlo, la IA de Sami elige la primera plantilla sin costo")
    void conElProveedorGlobalSimuladoYSinPisarloLaIaDeSamiEligeLaPrimeraPlantillaSinCosto() {
        cuandoSeProponeConSami(null, AVANZADO, "simulado");

        entoncesSeProponeInventarioSinCosto();
    }

    @Test
    @DisplayName("Si Sami pisa el proveedor con bedrock usa el modelo rápido y escala al avanzado cuando no está seguro")
    void siSamiPisaElProveedorConBedrockUsaElModeloRapidoYEscalaAlAvanzadoCuandoNoEstaSeguro() {
        dadoUnBedrockQueResponde(DUDOSA);

        cuandoSeProponeConSami("bedrock", AVANZADO, "simulado");

        entoncesSeEscalaDelRapidoAlAvanzado();
    }

    @Test
    @DisplayName("Sin modelo avanzado ni en Sami ni en el proveedor nunca escala")
    void sinModeloAvanzadoNiEnSamiNiEnElProveedorNuncaEscala() {
        dadoUnBedrockQueResponde(DUDOSA);

        cuandoSeProponeConSami("bedrock", "  ", "simulado");

        entoncesSoloSeConsultaElModeloRapido();
    }

    @Test
    @DisplayName("Con el proveedor global anthropic y sin pisarlo, Sami usa Claude de la API de Anthropic")
    void conElProveedorGlobalAnthropicYSinPisarloSamiUsaClaudeDeLaApiDeAnthropic() {
        dadoUnAnthropicQueResponde("{\"plantilla\":\"Ventas\",\"confianza\":0.9,\"columnas\":[]}");

        cuandoSeProponeConSami(null, null, "anthropic");

        entoncesSamiUsaClaudeDeAnthropic();
    }

    @Test
    @DisplayName("Con el proveedor simulado, la IA del taller no cuesta nada y pregunta con botonera")
    void conElProveedorSimuladoLaIaDelTallerNoCuestaNadaYPreguntaConBotonera() {
        cuandoSeConversaEnElTaller(null, AVANZADO, "simulado", "ayuda");

        entoncesElTallerEsSimuladoYPreguntaConBotonera();
    }

    @Test
    @DisplayName("Si Sami pisa el proveedor con simulado, el taller no llama al proveedor global")
    void siSamiPisaElProveedorConSimuladoElTallerNoLlamaAlProveedorGlobal() {
        cuandoSeConversaEnElTaller("simulado", null, "bedrock", "ayuda");

        entoncesElTallerEsSimulado();
    }

    @Test
    @DisplayName("Con el proveedor global bedrock, el taller usa el modelo rápido, una sola llamada y las quince herramientas")
    void conElProveedorGlobalBedrockElTallerUsaElModeloRapidoUnaSolaLlamadaYLasQuinceHerramientas() {
        dadoUnBedrockQueResponde("Hola");

        cuandoSeConversaEnElTaller(null, AVANZADO, "bedrock", "hola");

        entoncesElTallerUsaElModeloRapidoConLasQuinceHerramientasEnBedrock();
    }

    @Test
    @DisplayName("Con el proveedor global anthropic, el taller manda las quince herramientas a la API de Anthropic")
    void conElProveedorGlobalAnthropicElTallerMandaLasQuinceHerramientasALaApiDeAnthropic() {
        dadoUnAnthropicQueResponde("Hola");

        cuandoSeConversaEnElTaller(null, null, "anthropic", "hola");

        entoncesElTallerManda15HerramientasAAnthropic();
    }

    @Test
    @DisplayName("Carga el catálogo de componentes generado por el frontend con sus diez tipos")
    void cargaElCatalogoDeComponentesGeneradoPorElFrontendConSusDiezTipos() {
        cuandoSeCargaElCatalogo();

        entoncesElCatalogoTieneLosDiezTiposConSuSingular();
    }

    @Test
    @DisplayName("Carga los diez kits por rubro con ids únicos y términos en minúscula y sin tildes")
    void cargaLosDiezKitsPorRubroConIdsUnicosYTerminosEnMinusculaYSinTildes() {
        cuandoSeCarganLosKits();

        entoncesSeCarganLosDiezKitsConTerminosNormalizados();
    }

    @Test
    @DisplayName("Cada pieza de un kit usa un componente del catálogo, con datos válidos y sin repetir tipos")
    void cadaPiezaDeUnKitUsaUnComponenteDelCatalogoConDatosValidosYSinRepetirTipos() {
        cuandoSeCarganElCatalogoYLosKits();

        entoncesLasPiezasDeLosKitsUsanComponentesDelCatalogoConDatosValidos();
    }

    @Test
    @DisplayName("Las conexiones de cada kit unen dos piezas distintas del mismo kit y tienen etiqueta")
    void lasConexionesDeCadaKitUnenDosPiezasDistintasDelMismoKitYTienenEtiqueta() {
        cuandoSeCarganLosKits();

        entoncesLasConexionesUnenPiezasDistintasDelMismoKit();
    }

    @Test
    @DisplayName("Cada kit se arma entero, sin descartes, sobre un sistema vacío")
    void cadaKitSeArmaEnteroSinDescartesSobreUnSistemaVacio() {
        cuandoSeCarganLosKits();

        entoncesCadaKitSeArmaEnteroSobreUnSistemaVacio();
    }

    @Test
    @DisplayName("Cada kit se reconoce por su propio nombre, que es lo que ofrece la botonera")
    void cadaKitSeReconocePorSuPropioNombreQueEsLoQueOfreceLaBotonera() {
        cuandoSeCarganLosKits();

        entoncesCadaKitSeReconocePorSuNombre();
    }

    @Test
    @DisplayName("Las herramientas del taller del recurso coinciden con las que valida el dominio")
    void lasHerramientasDelTallerDelRecursoCoincidenConLasQueValidaElDominio() throws Exception {
        cuandoSeLeenLasHerramientasDelRecurso();

        entoncesLasHerramientasDelRecursoCoincidenConLasDelDominio();
    }

    // --- dado ---

    private void dadoUnBedrockQueResponde(String texto) {
        bedrock = bedrockQueResponde(texto);
    }

    private void dadoUnAnthropicQueResponde(String texto) {
        anthropic = anthropicQueResponde(texto);
    }

    // --- cuando ---

    private void cuandoSeCargaElDiccionario() {
        diccionario = config.diccionarioSami(JsonMapper.builder().build());
    }

    private void cuandoSeProponeConSami(String proveedorDeSami, String avanzadoDeSami, String proveedorGlobal) {
        propuesta = config.samiIaPort(config.promptSami(), JsonMapper.builder().build(),
                sami(proveedorDeSami, avanzadoDeSami), global(proveedorGlobal), proveedores()).proponer(consulta);
    }

    private void cuandoSeConversaEnElTaller(String proveedorDeSami, String avanzadoDeSami, String proveedorGlobal,
                                            String mensaje) {
        resultadoDelTaller = config.tallerIaPort(promptTaller(), JsonMapper.builder().build(),
                sami(proveedorDeSami, avanzadoDeSami), global(proveedorGlobal), proveedores())
                .conversar(consultaDeTaller(mensaje));
    }

    private void cuandoSeCargaElCatalogo() {
        catalogo = config.catalogoComponentes(JsonMapper.builder().build());
    }

    private void cuandoSeCarganLosKits() {
        kits = kits();
    }

    private void cuandoSeCarganElCatalogoYLosKits() {
        cuandoSeCargaElCatalogo();
        cuandoSeCarganLosKits();
    }

    private void cuandoSeLeenLasHerramientasDelRecurso() throws Exception {
        definiciones = JsonMapper.builder().build().readValue(
                getClass().getResourceAsStream("/ia/herramientas-taller.json"), DefinicionHerramienta[].class);
    }

    // --- entonces ---

    private void entoncesElDiccionarioTienePlantillasTerminosColumnasYPreguntas() {
        assertThat(diccionario.plantillas()).extracting("plantilla")
                .containsExactly("Ventas", "Inventario", "Pedidos", "Clientes", "Gastos");
        assertThat(diccionario.plantillas()).allSatisfy(plantilla -> assertThat(plantilla.terminos()).isNotEmpty());
        assertThat(diccionario.columnas()).extracting("nombre").contains("Saldo pendiente", "Forma de pago");
        assertThat(diccionario.preguntas()).extracting("id").contains("principal", "venta-o-stock");
    }

    private void entoncesElDiccionarioSoloReferenciaPlantillasConocidas() {
        List<String> nombres = diccionario.plantillas().stream().map(p -> p.plantilla()).toList();
        assertThat(diccionario.columnas()).allSatisfy(columna -> assertThat(nombres).containsAll(columna.plantillas()));
        assertThat(diccionario.preguntas()).allSatisfy(pregunta -> assertThat(pregunta.opciones())
                .allSatisfy(opcion -> assertThat(opcion.plantilla() == null || nombres.contains(opcion.plantilla()))
                        .isTrue()));
    }

    private void entoncesSeProponeInventarioSinCosto() {
        assertThat(propuesta.plantilla()).isEqualTo("Inventario");
        assertThat(propuesta.consumo().proveedor()).isEqualTo("simulado");
    }

    private void entoncesSeEscalaDelRapidoAlAvanzado() {
        assertThat(propuesta.consumo().modelo()).isEqualTo(RAPIDO + " + " + AVANZADO);
        assertThat(modelosConsultados(2)).containsExactly(RAPIDO, AVANZADO);
    }

    private void entoncesSoloSeConsultaElModeloRapido() {
        assertThat(modelosConsultados(1)).containsExactly(RAPIDO);
    }

    private void entoncesSamiUsaClaudeDeAnthropic() {
        assertThat(propuesta.plantilla()).isEqualTo("Ventas");
        assertThat(propuesta.consumo().proveedor()).isEqualTo("anthropic");
        assertThat(propuesta.consumo().modelo()).isEqualTo("haiku");
    }

    private void entoncesElTallerEsSimuladoYPreguntaConBotonera() {
        entoncesElTallerEsSimulado();
        assertThat(resultadoDelTaller.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
    }

    private void entoncesElTallerEsSimulado() {
        assertThat(resultadoDelTaller.consumo().proveedor()).isEqualTo("simulado");
    }

    private void entoncesElTallerUsaElModeloRapidoConLasQuinceHerramientasEnBedrock() {
        assertThat(resultadoDelTaller.texto()).isEqualTo("Hola");
        assertThat(resultadoDelTaller.consumo().modelo()).isEqualTo(RAPIDO);
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        assertThat(captor.getValue().inferenceConfig().maxTokens()).isEqualTo(400);
        assertThat(captor.getValue().toolConfig().tools())
                .hasSize(15)
                .extracting(herramienta -> herramienta.toolSpec().name())
                .containsExactly(HERRAMIENTAS);
    }

    private void entoncesElTallerManda15HerramientasAAnthropic() {
        assertThat(resultadoDelTaller.consumo().proveedor()).isEqualTo("anthropic");
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(anthropic.messages()).create(captor.capture());
        assertThat(captor.getValue().tools().orElseThrow())
                .extracting(herramienta -> herramienta.asTool().name())
                .containsExactly(HERRAMIENTAS);
    }

    private void entoncesElCatalogoTieneLosDiezTiposConSuSingular() {
        assertThat(catalogo.componentes()).extracting(ComponenteCatalogo::tipo).containsExactly(
                "inventory", "orders", "clients", "sales", "expenses", "summary", "form", "note", "entries", "exits");
        assertThat(catalogo.componentes()).allSatisfy(componente -> assertThat(componente.singular()).isNotBlank());
    }

    private void entoncesSeCarganLosDiezKitsConTerminosNormalizados() {
        assertThat(kits.kits()).extracting(KitRubro::id).containsExactly("almacen", "dietetica", "velas",
                "panaderia", "viandas", "ropa", "peluqueria", "taller-mecanico", "ferreteria", "libreria");
        assertThat(kits.kits()).allSatisfy(kit -> assertThat(kit.terminos())
                .isNotEmpty()
                .allMatch(termino -> termino.matches("[a-z0-9 ]+")));
    }

    private void entoncesLasPiezasDeLosKitsUsanComponentesDelCatalogoConDatosValidos() {
        for (KitRubro kit : kits.kits()) {
            List<String> tipos = kit.piezas().stream().map(PiezaKit::tipo).toList();
            assertThat(tipos).as("tipos de %s", kit.id()).allMatch(tipo -> catalogo.buscar(tipo).isPresent());
            assertThat(tipos.stream().filter(tipo -> !tipo.equals(CatalogoComponentes.TIPO_A_MEDIDA)))
                    .as("tipos que no se repiten en %s", kit.id()).doesNotHaveDuplicates();
            assertThat(kit.piezas()).extracting(PiezaKit::nombre).as("nombres de %s", kit.id())
                    .doesNotHaveDuplicates()
                    .allMatch(nombre -> nombre != null && !nombre.isBlank());
            kit.piezas().forEach(pieza -> assertQueLaPiezaEsValida(kit, pieza));
        }
    }

    private void assertQueLaPiezaEsValida(KitRubro kit, PiezaKit pieza) {
        List<String> etiquetas = etiquetas(pieza);
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

    private void entoncesLasConexionesUnenPiezasDistintasDelMismoKit() {
        for (KitRubro kit : kits.kits()) {
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

    private void entoncesCadaKitSeArmaEnteroSobreUnSistemaVacio() {
        TallerDePrueba taller = new TallerDePrueba(kits);
        for (KitRubro kit : kits.kits()) {
            ValidacionTaller result = taller.validar(
                    List.of(new LlamadaHerramienta("usar_kit", Map.of("rubro", kit.id()))),
                    new EstadoTaller("Mi sistema", "mosaic", List.of()));

            // Con clientes, el dato Cliente de las ventas va al lugar 2: una edición más.
            boolean conClientes = kit.piezas().stream().anyMatch(pieza -> pieza.tipo().equals("clients"));
            long ediciones = conClientes ? kit.piezas().stream()
                    .filter(pieza -> pieza.tipo().equals("sales")).count() : 0;
            assertThat(result.acciones()).as("acciones de %s", kit.id())
                    .hasSize(kit.piezas().size() + kit.conexiones().size() + (int) ediciones);
            List<Integer> ejemplos = result.acciones().stream()
                    .filter(accion -> accion.herramienta().equals("agregar_pieza"))
                    .map(accion -> ((List<?>) accion.argumentos().get("ejemplos")).size())
                    .toList();
            assertThat(ejemplos).as("ejemplos de %s", kit.id())
                    .isEqualTo(kit.piezas().stream().map(pieza -> pieza.ejemplos().size()).toList());
        }
    }

    private void entoncesCadaKitSeReconocePorSuNombre() {
        TallerDePrueba taller = new TallerDePrueba(kits);
        for (KitRubro kit : kits.kits()) {
            assertThat(taller.rubro(kit.nombre(), new EstadoTaller(null, "mosaic", List.of()), List.of()))
                    .as("rubro de «%s»", kit.nombre())
                    .map(KitRubro::id)
                    .contains(kit.id());
        }
    }

    private void entoncesLasHerramientasDelRecursoCoincidenConLasDelDominio() {
        assertThat(definiciones).extracting(DefinicionHerramienta::name).containsExactlyInAnyOrder(
                Arrays.stream(HerramientaTaller.values()).map(HerramientaTaller::nombre).toArray(String[]::new));
        assertThat(definiciones).allSatisfy(definicion -> assertThat(definicion.inputSchema()).containsKey("type"));
    }

    // --- datos ---

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
    private List<String> etiquetas(PiezaKit pieza) {
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

    /** Lo de Sami: el proveedor que pisa (nulo = el global) y el modelo avanzado que pisa. */
    private SamiProperties sami(String proveedor, String modeloAvanzado) {
        return new SamiProperties(0.7, 3, 20, 60, new SamiProperties.Ia(
                proveedor, null, null, null, modeloAvanzado, Duration.ofSeconds(30), 700, 0.6, 400));
    }

    /** Global con Bedrock (sin modelo avanzado: lo pone Sami) y Anthropic configurados. */
    private IaProperties global(String proveedor) {
        return new IaProperties(proveedor, null, Duration.ofSeconds(60), 10, 0.8, Map.of(
                "bedrock", new IaProperties.Proveedor(null, "us-east-1", null, RAPIDO, null, "vision"),
                "anthropic", new IaProperties.Proveedor(CLAVE, null, null, "haiku", null, "haiku")));
    }

    private ProveedoresIa proveedores() {
        return new ProveedoresIa(List.of(
                new ProveedorIaSimulado(new ClassPathResource("ia/lectura-simulada.json"), this::kits),
                new ProveedorIaBedrock(ajustes -> bedrock),
                new ProveedorIaAnthropic(ajustes -> anthropic)));
    }

    private List<String> modelosConsultados(int veces) {
        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock, times(veces)).converse(captor.capture());
        return captor.getAllValues().stream().map(ConverseRequest::modelId).toList();
    }
}
