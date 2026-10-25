package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversarEnTallerTest {

    private static final UUID NEGOCIO_ID = TallerDePrueba.NEGOCIO_ID.valor();
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

    private ConversarEnTaller conversacion;

    @BeforeEach
    void setUp() {
        conversacion = TallerDePrueba.conversacion(tallerIaPort, consumoSamiRepository, SamiPruebas.kitsReales(),
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC), LIMITE);
    }

    @Test
    @DisplayName("Reserva, consulta a la IA, registra el consumo y devuelve el texto con las acciones validadas")
    void executeShouldReserveCallRegisterAndValidate() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Listo, sumé gastos.", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("borrar_todo", Map.of())), CONSUMO));

        RespuestaTaller result = conversar("sumá gastos", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Listo, sumé gastos.");
        assertThat(result.acciones()).hasSize(1);
        assertThat(result.acciones().getFirst().herramienta()).isEqualTo("agregar_pieza");
        assertThat(result.preguntas()).isEmpty();
        verify(consumoSamiRepository).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Si Sami ya preguntó en su texto sin botonera, no suma la pregunta del kit; con botonera vale la suya")
    void executeShouldNotAddKitQuestionWhenTheModelAlreadyAsked() {
        givenQuotaAvailable();
        LlamadaHerramienta kit = new LlamadaHerramienta("usar_kit", Map.of("rubro", "peluqueria"));
        LlamadaHerramienta botonera = new LlamadaHerramienta("ofrecer_opciones",
                Map.of("pregunta", "¿Sumamos productos?", "opciones", List.of("Sí", "No")));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Armé turnos. ¿Le agrego algo?", List.of(kit), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé turnos. ¿Sumamos productos?", List.of(kit, botonera), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé turnos.", List.of(kit), CONSUMO));

        RespuestaTaller preguntoEnElTexto = conversar("peluquería", null, SIN_PIEZAS, HISTORIAL);
        RespuestaTaller usoLaBotonera = conversar("peluquería", null, SIN_PIEZAS, HISTORIAL);
        RespuestaTaller noPregunto = conversar("peluquería", null, SIN_PIEZAS, HISTORIAL);

        assertThat(preguntoEnElTexto.preguntas()).isEmpty();
        assertThat(usoLaBotonera.preguntas().getFirst().pregunta()).isEqualTo("¿Sumamos productos?");
        assertThat(noPregunto.preguntas().getFirst().pregunta()).isEqualTo("¿Atendés vos o trabajan más personas en el local?");
    }

    @Test
    @DisplayName("Saca las negritas de markdown: la charla muestra texto plano")
    void executeShouldRemoveMarkdownBold() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(
                " Tenés **Turnos** y **Clientes**. ", List.of(), CONSUMO));

        RespuestaTaller result = conversar("peluquería", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Tenés Turnos y Clientes.");
    }

    @Test
    @DisplayName("Le pasa a la IA el mensaje, el estado, la charla reciente y el kit del rubro reconocido")
    void executeShouldSendMessageStateHistoryAndKit() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        conversar("vendo velas", null, ESTADO, HISTORIAL);

        ConsultaTaller consulta = consultaEnviada();
        assertThat(consulta.mensaje()).isEqualTo("vendo velas");
        assertThat(consulta.estado()).isEqualTo(ESTADO);
        assertThat(consulta.historial()).isEqualTo(HISTORIAL);
        assertThat(consulta.kit().id()).isEqualTo("velas");
    }

    @Test
    @DisplayName("Si no reconoce el rubro no le pasa ningún kit")
    void executeShouldSendNoKitWhenRubroIsUnknown() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        conversar("hola", null, SIN_PIEZAS, List.of());

        assertThat(consultaEnviada()).isEqualTo(new ConsultaTaller("hola", SIN_PIEZAS, List.of()));
    }

    @Test
    @DisplayName("Si el modelo sólo usó un kit, cuenta cada pieza y cada conexión por su nombre")
    void executeShouldDescribeKitExpansionByName() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("usar_kit", Map.of("rubro", "peluqueria"))), CONSUMO));

        RespuestaTaller result = conversar("peluquería", null, SIN_PIEZAS, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Sumé turnos. Sumé servicios. Sumé clientes."
                + " Conecté turnos con clientes. Conecté turnos con servicios. Mirá cómo queda."
                + " ¿Atendés vos o trabajan más personas en el local?");
        assertThat(result.acciones()).hasSize(5);
    }

    @Test
    @DisplayName("Cuenta cuántos registros cargó y en qué pieza, en singular o en plural")
    void executeShouldDescribeLoadedRecords() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                        Map.of("Producto", "Yerba"), Map.of("Producto", "Azúcar"), Map.of("Producto", "Té")))),
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "expenses", "filas", List.of(
                        Map.of("Concepto", "Luz"))))), CONSUMO));

        RespuestaTaller result = conversar("te paso la foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Cargué 3 registros en inventario. Sumé gastos."
                + " Cargué 1 registro en gastos. Mirá cómo queda.");
    }

    @Test
    @DisplayName("La foto llega a la IA en el turno y en cada reintento, y el turno cuenta una sola consulta del día")
    void executeShouldSendImageToAiAndReserveOnce() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Cargué tus ventas.", List.of(), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(), CONSUMO));

        conversar("te paso la foto", FOTO, ESTADO, HISTORIAL);

        // Turno, reintento por «dijo que hizo sin hacer», pedido de los registros y pedido de las dudas de la foto.
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(4)).conversar(captor.capture());
        assertThat(captor.getAllValues()).extracting(ConsultaTaller::imagen).containsExactly(FOTO, FOTO, FOTO, FOTO);
        verify(consumoSamiRepository, times(1)).reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(4)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Con foto, si armó las piezas sin cargar lo que lee, se le piden los registros viendo las piezas armadas")
    void executeShouldAskForThePhotoRecordsOnTheBuiltPieces() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Te armo las ventas.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(
                        new LlamadaHerramienta("cargar_registros", Map.of("pieza", "Ventas", "filas", List.of(
                                Map.of("Producto", "Vela lavanda", "Cantidad", 2)))),
                        new LlamadaHerramienta("quitar_pieza", Map.of("pieza", "e1"))), CONSUMO));

        RespuestaTaller result = conversar("te paso la foto", FOTO, ESTADO, HISTORIAL);

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "cargar_registros");
        assertThat(result.texto()).isEqualTo("Te armo las ventas. Cargué 1 registro en ventas.");
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(3)).conversar(captor.capture());
        ConsultaTaller segunda = captor.getAllValues().get(1);
        assertThat(segunda.mensaje()).isEqualTo(ConversarEnTaller.PEDIDO_REGISTROS);
        assertThat(segunda.estado().piezas()).extracting(PiezaTaller::nombre).containsExactly("Inventario", "Ventas");
        assertThat(segunda.kit()).isNull();
        assertThat(captor.getAllValues().get(2).mensaje()).isEqualTo(ConversarEnTaller.PEDIDO_DUDAS);
        verify(consumoSamiRepository, times(3)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Los ejemplos del kit sólo se conservan al armar un sistema nuevo sin foto")
    void executeShouldKeepKitExamplesOnlyForANewSystemWithoutPhoto() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Armé tus velas.", List.of(
                new LlamadaHerramienta("usar_kit", Map.of("rubro", "velas"))), CONSUMO));

        RespuestaTaller nuevo = conversar("velas", null, SIN_PIEZAS, HISTORIAL);
        RespuestaTaller conFoto = conversar("velas", FOTO, SIN_PIEZAS, HISTORIAL);
        RespuestaTaller ajustando = conversar("velas", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(nuevo.acciones()).anyMatch(accion -> accion.argumentos().containsKey("ejemplos"));
        assertThat(conFoto.acciones()).noneMatch(accion -> accion.argumentos().containsKey("ejemplos"));
        assertThat(ajustando.acciones()).noneMatch(accion -> accion.argumentos().containsKey("ejemplos"));
    }

    @Test
    @DisplayName("Con foto, la duda del segundo pedido va a la botonera salvo que el modelo ya hubiera preguntado")
    void executeShouldUseThePhotoQuestionOnlyWhenTheModelDidNotAskIt() {
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

        RespuestaTaller sinPreguntar = conversar("foto", FOTO, ESTADO, HISTORIAL);
        RespuestaTaller preguntando = conversar("foto", FOTO, ESTADO, HISTORIAL);

        assertThat(sinPreguntar.preguntas().getFirst().pregunta()).isEqualTo("¿Qué significan los tildes?");
        assertThat(preguntando.preguntas().getFirst().pregunta()).isEqualTo("¿Qué significan los tildes?");
    }

    @Test
    @DisplayName("Con foto, si ya cargó registros sólo se le piden las dudas; sin foto, no se le pide nada")
    void executeShouldNotAskForRecordsWhenAlreadyLoadedOrWithoutPhoto() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(new LlamadaHerramienta("cargar_registros",
                        Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba"))))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Hola", List.of(), CONSUMO));

        conversar("te paso la foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);
        conversar("hola", null, ESTADO, HISTORIAL);

        verify(tallerIaPort, times(3)).conversar(any());
    }

    @Test
    @DisplayName("Con foto, si el modelo no dejó dudas se le piden juntas; si ya preguntó en el texto, no")
    void executeShouldAskForTheDoubtsOfThePhotoOnlyWhenTheModelLeftNone() {
        givenQuotaAvailable();
        LlamadaHerramienta registros = new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1",
                "filas", List.of(Map.of("Producto", "Yerba"))));
        LlamadaHerramienta dudas = new LlamadaHerramienta("ofrecer_opciones", Map.of("preguntas", List.of(
                Map.of("tema", "Tildes", "pregunta", "¿Qué significan los tildes?", "opciones", List.of("Entregado", "Pagado")),
                Map.of("tema", "Número", "pregunta", "¿Qué es el número de la derecha?", "opciones", List.of("Costo", "Deuda")))));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Cargué lo que leí.", List.of(registros), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(dudas), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Cargué lo que leí. ¿Los tildes son pagos?", List.of(registros), CONSUMO));

        RespuestaTaller sinDudas = conversar("foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);
        RespuestaTaller yaPregunto = conversar("foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(sinDudas.preguntas()).extracting("tema").containsExactly("Tildes", "Número");
        assertThat(yaPregunto.preguntas()).isEmpty();
        verify(tallerIaPort, times(3)).conversar(any());
    }

    @Test
    @DisplayName("Con foto, si el segundo pedido tampoco carga registros, queda lo del primero")
    void executeShouldKeepTheFirstAnswerWhenNoRecordsComeBack() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Te armo gastos.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("No se lee nada.", List.of(), CONSUMO));

        RespuestaTaller result = conversar("foto", FOTO, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Te armo gastos.");
        assertThat(result.acciones()).extracting(AccionTaller::herramienta).containsExactly("agregar_pieza");
    }

    @Test
    @DisplayName("Cuenta que borró registros, con los criterios en palabras")
    void executeShouldDescribeDeletedRecords() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                        Map.of("Producto", "Yerba"), Map.of("Producto", "Fideos"))))), CONSUMO));

        RespuestaTaller result = conversar("borrá", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto())
                .isEqualTo("Borré los registros de inventario donde Producto es Yerba, o Producto es Fideos. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Si dice que borró pero no invocó la herramienta, se le pide que lo haga")
    void executeShouldRetryWhenItClaimsToHaveDeletedWithoutTheTool() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Listo, borré las ventas de Anto.", List.of(), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Eliminé todo.", List.of(), CONSUMO));

        RespuestaTaller result = conversar("borrá", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        verify(tallerIaPort, times(2)).conversar(any());
    }

    @Test
    @DisplayName("Si algunos renglones no se pudieron cargar, Sami lo dice en vez de afirmar que cargó todo")
    void executeShouldAdmitRecordsItCouldNotLoad() {
        givenQuotaAvailable();
        LlamadaHerramienta registros = new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Yerba"), Map.of("Cantidad", 2), Map.of("Cantidad", 3))));
        LlamadaHerramienta otra = new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Fideos"), Map.of("Cantidad", 2))));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Cargué todo.", List.of(registros), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Cargué todo.", List.of(otra), CONSUMO));

        RespuestaTaller varios = conversar("foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);
        RespuestaTaller uno = conversar("foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(varios.texto()).endsWith("No pude cargar 2 renglones de la foto: faltan en la tabla, revisalos.");
        assertThat(uno.texto()).endsWith("No pude cargar 1 renglón de la foto: faltan en la tabla, revisalos.");
    }

    @Test
    @DisplayName("Si el segundo pedido de la foto también descarta renglones, se suman a los del primero")
    void executeShouldAddUpRecordsDroppedInTheSecondRequest() {
        givenQuotaAvailable();
        LlamadaHerramienta registros = new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Yerba"), Map.of("Cantidad", 2))));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Armé.", List.of(
                        new LlamadaHerramienta("renombrar_sistema", Map.of("nombre", "Mis ventas"))), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(registros), CONSUMO));

        RespuestaTaller result = conversar("foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto()).endsWith("No pude cargar 1 renglón de la foto: faltan en la tabla, revisalos.");
    }

    @Test
    @DisplayName("Si borra para recargar, como pidió la persona, y no carga, se le pide una vez que cargue lo corregido")
    void executeShouldAskToReloadWhenItOnlyDeleted() {
        givenQuotaAvailable();
        LlamadaHerramienta borrar = new LlamadaHerramienta("borrar_registros",
                Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba")));
        LlamadaHerramienta cargar = new LlamadaHerramienta("cargar_registros",
                Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba Playadito"))));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Borro y recargo.", List.of(borrar), CONSUMO))
                .thenReturn(new ResultadoTallerIa("", List.of(cargar), CONSUMO));

        RespuestaTaller result = conversar("borrá la yerba y cargala de nuevo como Yerba Playadito",
                null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("borrar_registros", "cargar_registros");
        assertThat(result.texto()).isEqualTo("Borro y recargo. Cargué 1 registro en inventario.");
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(2)).conversar(captor.capture());
        assertThat(captor.getAllValues().get(1).mensaje()).startsWith(ConversarEnTaller.PEDIDO_RECARGA)
                .endsWith("como Yerba Playadito");
    }

    @Test
    @DisplayName("Si sólo pidió borrar, no se le pide cargar nada; y si el pedido extra no carga, queda el borrado")
    void executeShouldNotAskToReloadWhenOnlyDeletingWasRequested() {
        givenQuotaAvailable();
        LlamadaHerramienta borrar = new LlamadaHerramienta("borrar_registros",
                Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba")));
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(borrar), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Listo.", List.of(borrar), CONSUMO))
                .thenReturn(new ResultadoTallerIa("No sé.", List.of(), CONSUMO));

        RespuestaTaller soloBorrar = conversar("borrá la yerba", null, ESTADO_CON_DATOS, HISTORIAL);
        RespuestaTaller sinRecarga = conversar("borrala y cargala otra vez", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(soloBorrar.acciones()).hasSize(1);
        assertThat(sinRecarga.acciones()).hasSize(1);
        assertThat(sinRecarga.texto()).isEqualTo("Listo.");
        verify(tallerIaPort, times(3)).conversar(any());
    }

    @Test
    @DisplayName("Si borra y carga en la misma respuesta, no se le pide nada más")
    void executeShouldNotAskAgainWhenItDeletedAndReloadedTogether() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("Listo.", List.of(
                new LlamadaHerramienta("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba"))),
                new LlamadaHerramienta("cargar_registros", Map.of("pieza", "e1",
                        "filas", List.of(Map.of("Producto", "Yerba Playadito"))))), CONSUMO));

        RespuestaTaller result = conversar("borrala y cargala de nuevo", null, ESTADO_CON_DATOS,
                HISTORIAL);

        assertThat(result.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("borrar_registros", "cargar_registros");
        verify(tallerIaPort, times(1)).conversar(any());
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void executeShouldNotCallAiWhenLimitIsReached() {
        when(consumoSamiRepository.reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> conversar("hola", null, ESTADO, HISTORIAL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ConversarEnTaller.LIMITE_ALCANZADO);
        verify(tallerIaPort, never()).conversar(any());
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple")
    void executeShouldTranslateAiFailure() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> conversar("hola", null, ESTADO, HISTORIAL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ConversarEnTaller.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si el modelo no escribió texto usa la pregunta de la botonera")
    void executeShouldUseQuestionWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("ofrecer_opciones", Map.of(
                        "pregunta", "¿Qué sumamos?", "opciones", List.of("Gastos", "Clientes")))), CONSUMO));

        RespuestaTaller result = conversar("ayuda", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo("¿Qué sumamos?");
        assertThat(result.preguntas().getFirst().opciones()).containsExactly("Gastos", "Clientes");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto cuenta lo que quedó hecho")
    void executeShouldDescribeChangesWhenThereIsNoText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  ", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO));
        RespuestaTaller uno = conversar("gastos", null, ESTADO, HISTORIAL);
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("", List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("cambiar_distribucion", Map.of("layout", "single"))), CONSUMO));

        RespuestaTaller dos = conversar("gastos", null, ESTADO, HISTORIAL);

        assertThat(uno.texto()).isEqualTo("Sumé gastos. Mirá cómo queda.");
        assertThat(dos.texto()).isEqualTo("Sumé gastos. Cambié la distribución. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe con el nombre de cada pieza las piezas y los datos que se tocaron")
    void executeShouldDescribePiecesAndFieldsByName() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("quitar_pieza", Map.of("pieza", "e2")),
                new LlamadaHerramienta("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Productos")),
                new LlamadaHerramienta("renombrar_sistema", Map.of("nombre", "Velas")),
                new LlamadaHerramienta("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Proveedor", "tipo", "text")),
                new LlamadaHerramienta("quitar_campo", Map.of("pieza", "e1", "campo", "proveedor"))), CONSUMO));

        RespuestaTaller result = conversar("ordená", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Quité clientes. Renombré inventario a Productos. Llamé al sistema Velas."
                + " Sumé Proveedor a inventario. Quité Proveedor de inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Cuenta qué dato cambió y en qué pieza, con la etiqueta que tenía")
    void executeShouldDescribeEditedField() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("editar_campo", Map.of("pieza", "e1", "campo", "producto", "etiqueta", "Artículo"))),
                CONSUMO));

        RespuestaTaller result = conversar("cambialo", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Cambié Producto en inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe una pieza que se sumó en el mismo turno con su nombre del catálogo")
    void executeShouldDescribePiecesAddedInTheSameTurn() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("agregar_campo", Map.of("pieza", "expenses", "etiqueta", "Rubro", "tipo", "text")),
                new LlamadaHerramienta("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Aparte"))), CONSUMO));

        RespuestaTaller result = conversar("gastos", null, ESTADO_CON_DATOS, HISTORIAL);

        assertThat(result.texto())
                .isEqualTo("Sumé gastos. Sumé Rubro a gastos. Pasé clientes a la sección Aparte. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto y nada de lo que pidió sirve, lo dice")
    void executeShouldAdmitItCouldNotDoAnythingWhenNothingIsValid() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(null, List.of(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "turnos"))), CONSUMO));

        RespuestaTaller result = conversar("turnos", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        assertThat(result.acciones()).isEmpty();
    }

    @Test
    @DisplayName("Lleva el texto del modelo sin espacios sobrantes")
    void executeShouldStripModelText() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa("  Hola  \n", List.of(), CONSUMO));

        assertThat(conversar("hola", null, ESTADO, HISTORIAL).texto()).isEqualTo("Hola");
    }

    @Test
    @DisplayName("Si dice que armó algo sin invocar herramientas, se le pide una vez que lo haga y vale el reintento")
    void executeShouldRetryWhenTheModelClaimsChangesWithoutTools() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Armé tu sistema con Arreglos y Clientes.", List.of(), CONSUMO))
                .thenReturn(new ResultadoTallerIa("Armé Gastos.", List.of(
                        new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), CONSUMO));

        RespuestaTaller result = conversar("llevo arreglos", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo("Armé Gastos.");
        assertThat(result.acciones()).hasSize(1);
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(2)).conversar(captor.capture());
        assertThat(captor.getAllValues().get(1).mensaje()).isEqualTo("llevo arreglos" + ConversarEnTaller.RECORDATORIO);
        verify(consumoSamiRepository, times(1)).reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(2)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Si después del reintento sigue diciendo que hizo algo sin hacerlo, contesta con honestidad")
    void executeShouldNotClaimChangesItDidNotMake() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any()))
                .thenReturn(new ResultadoTallerIa("Moví el cliente adelante.", List.of(), CONSUMO));

        RespuestaTaller result = conversar("poné el cliente primero", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        assertThat(result.acciones()).isEmpty();
        verify(tallerIaPort, times(2)).conversar(any());
    }

    @Test
    @DisplayName("Una explicación sin cambios no se reintenta")
    void executeShouldNotRetryAnExplanation() {
        givenQuotaAvailable();
        when(tallerIaPort.conversar(any())).thenReturn(new ResultadoTallerIa(
                "Ya está vinculado: al registrar una venta elegís la clienta de una lista.", List.of(), CONSUMO));

        RespuestaTaller result = conversar("¿están vinculadas?", null, ESTADO, HISTORIAL);

        assertThat(result.texto()).startsWith("Ya está vinculado");
        verify(tallerIaPort, times(1)).conversar(any());
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta a la IA")
    void executeShouldNotCallAiWhenBusinessIsMissing() {
        UUID usuarioSinNegocio = UUID.randomUUID();
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(UsuarioId.de(usuarioSinNegocio)))
                .thenThrow(new ResourceNotFoundException("Sin negocio"));
        ConversarEnTaller sinNegocio = new ConversarEnTaller(negocioRepository, tallerIaPort, consumoSamiRepository,
                SamiPruebas.catalogoReal(), SamiPruebas.kitsReales(), Clock.systemUTC(), LIMITE);
        ConversarEnTallerRequest request = new ConversarEnTallerRequest(UsuarioId.de(usuarioSinNegocio), "hola",
                new EstadoTallerItem("", "mosaic", List.of()), List.of(), null);

        assertThatThrownBy(() -> sinNegocio.execute(request)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tallerIaPort, consumoSamiRepository);
    }

    // --- arrange ---
    private void givenQuotaAvailable() {
        when(consumoSamiRepository.reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA)).thenReturn(true);
    }

    // --- act ---
    private RespuestaTaller conversar(String mensaje, ImagenTaller imagen, EstadoTaller estado,
                                      List<MensajeTaller> historial) {
        return TallerDePrueba.conversar(conversacion, mensaje, imagen, estado, historial);
    }

    // --- assert ---
    private ConsultaTaller consultaEnviada() {
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort).conversar(captor.capture());
        return captor.getValue();
    }
}
