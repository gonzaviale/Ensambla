package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TallerSamiServiceTest {

    private static final UUID NEGOCIO_ID = UUID.randomUUID();
    private static final int LIMITE = 60;
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 800, 90);
    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of())));
    private static final EstadoTaller ESTADO_CON_DATOS = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text"))),
            new PiezaTaller("e2", "clients", "Clientes", null, List.of())));
    private static final EstadoTaller SIN_PIEZAS = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final List<MensajeTaller> HISTORIAL = List.of(new MensajeTaller("sami", "Hola"));
    private static final ImagenTaller FOTO = new ImagenTaller("image/png", new byte[]{1, 2, 3});

    @Mock
    private TallerIaPort tallerIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    private TallerSamiService service;

    @BeforeEach
    void setUp() {
        CatalogoComponentes catalogo = SamiPruebas.catalogoReal();
        KitsRubro kits = SamiPruebas.kitsReales();
        service = new TallerSamiService(tallerIaPort, consumoSamiRepository,
                new HerramientasTallerService(catalogo, kits), new RubroTallerService(kits), catalogo);
    }

    @Test
    @DisplayName("Reserva, consulta a la IA, registra el consumo y devuelve el texto con las acciones validadas")
    void conversarShouldReserveCallRegisterAndValidate() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Listo, sumé gastos.", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("borrar_todo", Map.of())), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "sumá gastos", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Listo, sumé gastos.");
        assertThat(result.acciones()).hasSize(1);
        assertThat(result.acciones().getFirst().herramienta()).isEqualTo("agregar_pieza");
        assertThat(result.opciones()).isNull();
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Si Sami ya preguntó en su texto sin botonera, no suma la pregunta del kit; con botonera vale la suya")
    void conversarShouldNotAddKitQuestionWhenTheModelAlreadyAsked() {
        givenQuotaAvailable();
        LlamadaHerramienta kit = new LlamadaHerramienta("usar_kit", Map.of("rubro", "peluqueria"));
        LlamadaHerramienta botonera = new LlamadaHerramienta("ofrecer_opciones",
                Map.of("pregunta", "¿Sumamos productos?", "opciones", List.of("Sí", "No")));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Armé turnos. ¿Le agrego algo?", List.of(kit), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé turnos. ¿Sumamos productos?", List.of(kit, botonera), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé turnos.", List.of(kit), CONSUMO));

        RespuestaTaller preguntoEnElTexto = service.conversar(NEGOCIO_ID, "peluquería", null, SIN_PIEZAS, HISTORIAL, AHORA, LIMITE);
        RespuestaTaller usoLaBotonera = service.conversar(NEGOCIO_ID, "peluquería", null, SIN_PIEZAS, HISTORIAL, AHORA, LIMITE);
        RespuestaTaller noPregunto = service.conversar(NEGOCIO_ID, "peluquería", null, SIN_PIEZAS, HISTORIAL, AHORA, LIMITE);

        assertThat(preguntoEnElTexto.opciones()).isNull();
        assertThat(usoLaBotonera.opciones().pregunta()).isEqualTo("¿Sumamos productos?");
        assertThat(noPregunto.opciones().pregunta()).isEqualTo("¿Atendés vos o trabajan más personas en el local?");
    }

    @Test
    @DisplayName("Saca las negritas de markdown: la charla muestra texto plano")
    void conversarShouldRemoveMarkdownBold() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(
                " Tenés **Turnos** y **Clientes**. ", List.of(), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "peluquería", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Tenés Turnos y Clientes.");
    }

    @Test
    @DisplayName("Le pasa a la IA el mensaje, el estado, la charla reciente y el kit del rubro reconocido")
    void conversarShouldSendMessageStateHistoryAndKit() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        service.conversar(NEGOCIO_ID, "vendo velas", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        ConsultaTaller consulta = consultaEnviada();
        assertThat(consulta.mensaje()).isEqualTo("vendo velas");
        assertThat(consulta.estado()).isEqualTo(ESTADO);
        assertThat(consulta.historial()).isEqualTo(HISTORIAL);
        assertThat(consulta.kit().id()).isEqualTo("velas");
    }

    @Test
    @DisplayName("Si no reconoce el rubro no le pasa ningún kit")
    void conversarShouldSendNoKitWhenRubroIsUnknown() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        service.conversar(NEGOCIO_ID, "hola", null, SIN_PIEZAS, List.of(), AHORA, LIMITE);

        assertThat(consultaEnviada()).isEqualTo(new ConsultaTaller("hola", SIN_PIEZAS, List.of()));
    }

    @Test
    @DisplayName("Si el modelo sólo usó un kit, cuenta cada pieza y cada conexión por su nombre")
    void conversarShouldDescribeKitExpansionByName() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("usar_kit", Map.of("rubro", "peluqueria"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "peluquería", null, SIN_PIEZAS, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Sumé turnos. Sumé servicios. Sumé clientes."
                + " Conecté turnos con clientes. Conecté turnos con servicios. Mirá cómo queda."
                + " ¿Atendés vos o trabajan más personas en el local?");
        assertThat(result.acciones()).hasSize(5);
    }

    @Test
    @DisplayName("Cuenta cuántos registros cargó y en qué pieza, en singular o en plural")
    void conversarShouldDescribeLoadedRecords() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                        Map.of("Producto", "Yerba"), Map.of("Producto", "Azúcar"), Map.of("Producto", "Té")))),
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "expenses", "filas", List.of(
                        Map.of("Concepto", "Luz"))))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "te paso la foto", FOTO, ESTADO_CON_DATOS, HISTORIAL,
                AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Cargué 3 registros en inventario. Sumé gastos."
                + " Cargué 1 registro en gastos. Mirá cómo queda.");
    }

    @Test
    @DisplayName("La foto llega a la IA en el turno y en cada reintento, y el turno cuenta una sola consulta del día")
    void conversarShouldSendImageToAiAndReserveOnce() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Cargué tus ventas.", List.of(), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(), CONSUMO));

        service.conversar(NEGOCIO_ID, "te paso la foto", FOTO, ESTADO, HISTORIAL, AHORA, LIMITE);

        // Turno, reintento por «dijo que hizo sin hacer» y pedido de los registros de la foto.
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(3)).conversar(captor.capture());
        assertThat(captor.getAllValues()).extracting(ConsultaTaller::imagen).containsExactly(FOTO, FOTO, FOTO);
        verify(consumoSamiRepository, times(1)).reservar(NEGOCIO_ID, HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(3)).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Con foto, si armó las piezas sin cargar lo que lee, se le piden los registros viendo las piezas armadas")
    void conversarShouldAskForThePhotoRecordsOnTheBuiltPieces() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Te armo las ventas.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(
                        new LlamadaHerramienta("cargar_registros", Map.of("pieza", "Ventas", "filas", List.of(
                                Map.of("Producto", "Vela lavanda", "Cantidad", 2)))),
                        new LlamadaHerramienta("quitar_pieza", Map.of("pieza", "e1"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "te paso la foto", FOTO, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "cargar_registros");
        assertThat(result.texto()).isEqualTo("Te armo las ventas. Cargué 1 registro en ventas.");
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(2)).conversar(captor.capture());
        ConsultaTaller segunda = captor.getAllValues().get(1);
        assertThat(segunda.mensaje()).isEqualTo(TallerSamiService.PEDIDO_REGISTROS);
        assertThat(segunda.estado().piezas()).extracting(PiezaTaller::nombre).containsExactly("Inventario", "Ventas");
        assertThat(segunda.kit()).isNull();
        verify(consumoSamiRepository, times(2)).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Con foto, las filas de ejemplo de las piezas armadas se omiten; sin foto se conservan")
    void conversarShouldDropExamplesOnlyWithPhoto() {
        givenQuotaAvailable();
        ResultadoTallerIa armado = new ResultadoTallerIa("Armé las ventas.", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas",
                        "ejemplos", List.of(Map.of("Producto", "Vela lavanda")))),
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                        Map.of("Producto", "Yerba"))))), CONSUMO);
        when(tallerIaPort.conversar(any())).thenReturn(armado);

        RespuestaTaller conFoto = service.conversar(NEGOCIO_ID, "mirá", FOTO, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);
        RespuestaTaller sinFoto = service.conversar(NEGOCIO_ID, "mirá", null, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);

        assertThat(conFoto.acciones().get(0).argumentos()).doesNotContainKey("ejemplos").containsKey("nombre");
        assertThat(sinFoto.acciones().get(0).argumentos()).containsKey("ejemplos");
    }

    @Test
    @DisplayName("Con foto, la duda del segundo pedido va a la botonera salvo que el modelo ya hubiera preguntado")
    void conversarShouldUseThePhotoQuestionOnlyWhenTheModelDidNotAskIt() {
        givenQuotaAvailable();
        LlamadaHerramienta registros = new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1",
                "filas", List.of(Map.of("Producto", "Yerba"))));
        LlamadaHerramienta duda = new LlamadaHerramienta("ofrecer_opciones", Map.of(
                "pregunta", "¿Qué significan los tildes?", "opciones", List.of("Pagado", "Entregado")));
        LlamadaHerramienta otra = new LlamadaHerramienta("ofrecer_opciones", Map.of(
                "pregunta", "¿Otra cosa?", "opciones", List.of("Sí", "No")));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Veo un cuaderno.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(registros, duda), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Veo un cuaderno.", List.of(duda), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(registros, otra), CONSUMO));

        RespuestaTaller sinPreguntar = service.conversar(NEGOCIO_ID, "foto", FOTO, ESTADO, HISTORIAL, AHORA, LIMITE);
        RespuestaTaller preguntando = service.conversar(NEGOCIO_ID, "foto", FOTO, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(sinPreguntar.opciones().pregunta()).isEqualTo("¿Qué significan los tildes?");
        assertThat(preguntando.opciones().pregunta()).isEqualTo("¿Qué significan los tildes?");
    }

    @Test
    @DisplayName("Con foto, si ya cargó registros no se le pide nada más; sin foto, tampoco")
    void conversarShouldNotAskForRecordsWhenAlreadyLoadedOrWithoutPhoto() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(new LlamadaHerramienta("cargar_registros",
                        Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba"))))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        service.conversar(NEGOCIO_ID, "te paso la foto", FOTO, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);
        service.conversar(NEGOCIO_ID, "hola", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        verify(tallerIaPort, times(2)).conversar(any());
    }

    @Test
    @DisplayName("Con foto, si el segundo pedido tampoco carga registros, queda lo del primero")
    void conversarShouldKeepTheFirstAnswerWhenNoRecordsComeBack() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Te armo gastos.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("No se lee nada.", List.of(), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "foto", FOTO, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Te armo gastos.");
        assertThat(result.acciones()).extracting(AccionTaller::herramienta).containsExactly("agregar_pieza");
    }

    @Test
    @DisplayName("Cuenta que cargó ejemplos en una pieza")
    void conversarShouldDescribeLoadedExamples() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("cargar_ejemplos", Map.of(
                        "pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba"))))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "ejemplos", null, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Cargué ejemplos en inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void conversarShouldNotCallAiWhenLimitIsReached() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> service.conversar(NEGOCIO_ID, "hola", null, ESTADO, HISTORIAL, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(TallerSamiService.LIMITE_ALCANZADO);
        verify(tallerIaPort, never()).conversar(any());
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple")
    void conversarShouldTranslateAiFailure() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> service.conversar(NEGOCIO_ID, "hola", null, ESTADO, HISTORIAL, AHORA, LIMITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(TallerSamiService.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si el modelo no escribió texto usa la pregunta de la botonera")
    void conversarShouldUseQuestionWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("ofrecer_opciones", Map.of(
                        "pregunta", "¿Qué sumamos?", "opciones", List.of("Gastos", "Clientes")))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "ayuda", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("¿Qué sumamos?");
        assertThat(result.opciones().opciones()).containsExactly("Gastos", "Clientes");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto cuenta lo que quedó hecho")
    void conversarShouldDescribeChangesWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  ", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO));
        RespuestaTaller uno = service.conversar(NEGOCIO_ID, "gastos", null, ESTADO, HISTORIAL, AHORA, LIMITE);
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("cambiar_distribucion", Map.of("layout", "single"))), CONSUMO));

        RespuestaTaller dos = service.conversar(NEGOCIO_ID, "gastos", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(uno.texto()).isEqualTo("Sumé gastos. Mirá cómo queda.");
        assertThat(dos.texto()).isEqualTo("Sumé gastos. Cambié la distribución. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe con el nombre de cada pieza las piezas y los datos que se tocaron")
    void conversarShouldDescribePiecesAndFieldsByName() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("quitar_pieza", Map.of("pieza", "e2")),
                new LlamadaHerramienta("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Productos")),
                new LlamadaHerramienta("renombrar_sistema", Map.of("nombre", "Velas")),
                new LlamadaHerramienta("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Proveedor", "tipo", "text")),
                new LlamadaHerramienta("quitar_campo", Map.of("pieza", "e1", "campo", "proveedor"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "ordená", null, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Quité clientes. Renombré inventario a Productos. Llamé al sistema Velas."
                + " Sumé Proveedor a inventario. Quité Proveedor de inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Cuenta qué dato cambió y en qué pieza, con la etiqueta que tenía")
    void conversarShouldDescribeEditedField() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("editar_campo", Map.of("pieza", "e1", "campo", "producto", "etiqueta", "Artículo"))),
                CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "cambialo", null, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Cambié Producto en inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe una pieza que se sumó en el mismo turno con su nombre del catálogo")
    void conversarShouldDescribePiecesAddedInTheSameTurn() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("agregar_campo", Map.of("pieza", "expenses", "etiqueta", "Rubro", "tipo", "text")),
                new LlamadaHerramienta("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Aparte"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "gastos", null, ESTADO_CON_DATOS, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto())
                .isEqualTo("Sumé gastos. Sumé Rubro a gastos. Pasé clientes a la sección Aparte. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto y nada de lo que pidió sirve, lo dice")
    void conversarShouldAdmitItCouldNotDoAnythingWhenNothingIsValid() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "turnos"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "turnos", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo(TallerSamiService.SIN_CAMBIOS);
        assertThat(result.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Lleva el texto del modelo sin espacios sobrantes")
    void conversarShouldStripModelText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  Hola  \n", List.of(), CONSUMO));

        assertThat(service.conversar(NEGOCIO_ID, "hola", null, ESTADO, HISTORIAL, AHORA, LIMITE).texto()).isEqualTo("Hola");
    }

    @Test
    @DisplayName("Si dice que armó algo sin invocar herramientas, se le pide una vez que lo haga y vale el reintento")
    void conversarShouldRetryWhenTheModelClaimsChangesWithoutTools() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Armé tu sistema con Arreglos y Clientes.", List.of(), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé Gastos.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "llevo arreglos", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo("Armé Gastos.");
        assertThat(result.acciones()).hasSize(1);
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(2)).conversar(captor.capture());
        assertThat(captor.getAllValues().get(1).mensaje()).isEqualTo("llevo arreglos" + TallerSamiService.RECORDATORIO);
        verify(consumoSamiRepository, times(1)).reservar(NEGOCIO_ID, HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(2)).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Si después del reintento sigue diciendo que hizo algo sin hacerlo, contesta con honestidad")
    void conversarShouldNotClaimChangesItDidNotMake() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Moví el cliente adelante.", List.of(), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "poné el cliente primero", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).isEqualTo(TallerSamiService.SIN_CAMBIOS);
        assertThat(result.acciones()).isEmpty();
        verify(tallerIaPort, times(2)).conversar(any());
    }

    @Test
    @DisplayName("Una explicación sin cambios no se reintenta")
    void conversarShouldNotRetryAnExplanation() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(
                "Ya está vinculado: al registrar una venta elegís la clienta de una lista.", List.of(), CONSUMO));

        RespuestaTaller result = service.conversar(NEGOCIO_ID, "¿están vinculadas?", null, ESTADO, HISTORIAL, AHORA, LIMITE);

        assertThat(result.texto()).startsWith("Ya está vinculado");
        verify(tallerIaPort, times(1)).conversar(any());
    }

    private void givenQuotaAvailable() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE, AHORA)).thenReturn(true);
    }

    private ConsultaTaller consultaEnviada() {
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort).conversar(captor.capture());
        return captor.getValue();
    }
}
