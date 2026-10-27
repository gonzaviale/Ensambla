package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AtenderMensajeWhatsappTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-27T15:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRODUCTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId PRODUCTO_AJENO_ID = CampoId.de("c0000000-0000-4000-8000-000000000009");
    private static final RegistroId REGISTRO_ID = RegistroId.de("e0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491122334455";

    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class);
    private final PropuestaWhatsappRepository propuestaRepository = mock(PropuestaWhatsappRepository.class);
    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final InterpreteRegistrosIaPort interpreteIa = mock(InterpreteRegistrosIaPort.class);
    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeWhatsapp atender = new AtenderMensajeWhatsapp(mensajeRepository, vinculoRepository,
            propuestaRepository, procesamientoRepository, campoRepository, bloqueRepository, registroRepository,
            controlPlanService, consumoSamiRepository, interpreteIa, lectorCuadernoPort, mensajeria,
            new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC),
            new PoliticaAtencionWhatsapp(60, 100, 0.6, Duration.ofMinutes(2)));

    private final List<Registro> existentes = new java.util.ArrayList<>();
    private final TablaDisponible gastos = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");
    private final TablaDisponible ventas = new TablaDisponible(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
            new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.TEXTO, List.of()))), "Mi panadería");

    @BeforeEach
    void givenNegocioVinculadoConTablas() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA)));
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(gastos, ventas));
        when(consumoSamiRepository.reservar(eq(NEGOCIO_ID), any(), anyInt(), any())).thenReturn(true);
        when(propuestaRepository.save(any(PropuestaWhatsapp.class))).thenAnswer(i -> i.getArgument(0));
        when(mensajeRepository.vencidos(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Sin mensajes en la cola no hace nada y avisa que no había")
    void executeShouldReturnFalseWhenQueueIsEmpty() {
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.empty());

        assertThat(atender.execute()).isFalse();

        verifyNoInteractions(mensajeria, interpreteIa);
    }

    @Test
    @DisplayName("Un mensaje reclamado que nunca terminó se marca fallido y la persona recibe el aviso")
    void executeShouldFailLostMessages() {
        MensajeWhatsapp perdido = givenTexto("hola");
        when(mensajeRepository.vencidos(any())).thenReturn(List.of(perdido));
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.empty());

        atender.execute();

        assertThat(perdido.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
    }

    @Test
    @DisplayName("Si el número se desvinculó mientras esperaba en la cola, recibe las instrucciones")
    void executeShouldAnswerUnlinkedNumber() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
        MensajeWhatsapp mensaje = givenReclamado(givenTexto("gasté 5000"));

        assertThat(atender.execute()).isTrue();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_VINCULADO);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        verifyNoInteractions(interpreteIa);
    }

    @Test
    @DisplayName("Una nota de voz recibe la respuesta de que por ahora sólo se entienden textos y fotos")
    void executeShouldAnswerVoiceNote() {
        givenReclamado(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("A1")
                .tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").recibidoEn(AHORA).build());

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
    }

    // --- texto ---

    @Test
    @DisplayName("Un texto claro arma una propuesta pendiente con el resumen y descuenta una consulta de Sami")
    void executeShouldProposeRowsFromClearText() {
        MensajeWhatsapp mensaje = givenReclamado(givenTexto("gasté 5000 en harina"));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        PropuestaWhatsapp propuesta = thenPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(propuesta.getBloqueId()).isEqualTo(GASTOS_ID);
        assertThat(propuesta.getFilas().getFirst().valores())
                .containsEntry(MONTO_ID.toString(), "5000").containsEntry(CONCEPTO_ID.toString(), "harina");
        assertThat(thenRespuesta()).contains("Para *Gastos* (Mi panadería) entendí:", "Monto: 5000", "SÍ");
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        verify(consumoSamiRepository).registrar(eq(NEGOCIO_ID), any(), any(ConsumoIaSami.class), any());
    }

    @Test
    @DisplayName("Si la IA no está segura de la tabla, ofrece la lista numerada con la sugerida primero")
    void executeShouldAskTableWhenConfidenceIsLow() {
        givenReclamado(givenTexto("5000 de harina"));
        givenInterpretacion(GASTOS_ID, 0.3);

        atender.execute();

        PropuestaWhatsapp propuesta = thenPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
        assertThat(propuesta.getOpciones()).containsExactly(GASTOS_ID, VENTAS_ID);
        assertThat(thenRespuesta()).contains("Respondé con el número", "1) Mi panadería › Gastos");
    }

    @Test
    @DisplayName("Si ninguna tabla corresponde pide aclaración y no guarda ninguna propuesta")
    void executeShouldAskForClarificationWhenNoTableFits() {
        givenReclamado(givenTexto("hola"));
        when(interpreteIa.interpretar(any())).thenReturn(InterpretacionRegistros.sinResultado(consumo()));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
        verify(propuestaRepository, never()).save(any());
        verify(consumoSamiRepository).registrar(eq(NEGOCIO_ID), any(), any(ConsumoIaSami.class), any());
    }

    @Test
    @DisplayName("Con el límite diario de Sami agotado responde el motivo y no llama a la IA")
    void executeShouldRejectWhenSamiLimitIsReached() {
        MensajeWhatsapp mensaje = givenReclamado(givenTexto("gasté 5000 en harina"));
        when(consumoSamiRepository.reservar(eq(NEGOCIO_ID), any(), eq(60), any())).thenReturn(false);

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.LIMITE_SAMI);
        verifyNoInteractions(interpreteIa);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
    }

    @Test
    @DisplayName("Un negocio sin tablas recibe el aviso de que arme su sistema primero")
    void executeShouldExplainWhenBusinessHasNoTables() {
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of());
        givenReclamado(givenTexto("gasté 5000 en harina"));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SIN_TABLAS);
    }

    @Test
    @DisplayName("Un error inesperado deja el mensaje fallido, avisa a la persona y no se reintenta")
    void executeShouldFailTheMessageOnUnexpectedError() {
        MensajeWhatsapp mensaje = givenReclamado(givenTexto("gasté 5000 en harina"));
        when(interpreteIa.interpretar(any())).thenThrow(new IllegalStateException("se cayó el proveedor"));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).contains("IllegalStateException", "se cayó el proveedor");
    }

    // --- confirmación ---

    @Test
    @DisplayName("Responder sí guarda las filas al final de la tabla, marcadas como cargadas por WhatsApp")
    void executeShouldSaveRowsWhenUserConfirms() {
        givenReclamado(givenTexto("Sí"));
        PropuestaWhatsapp propuesta = givenPendiente(List.of(
                new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "5000"), Set.of()),
                new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "huevos", MONTO_ID.toString(), "2500"), Set.of())));
        givenTablaGuardable(3);

        atender.execute();

        ArgumentCaptor<List<Registro>> captor = ArgumentCaptor.forClass(List.class);
        verify(registroRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Registro::getOrden).containsExactly(3, 4);
        assertThat(captor.getValue()).extracting(Registro::getOrigen).containsOnly("WhatsApp");
        assertThat(captor.getValue().getFirst().getValores()).containsEntry(CONCEPTO_ID.toString(), "harina");
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.CONFIRMADA);
        assertThat(thenRespuesta()).contains("guardé 2 filas en *Gastos*");
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 2, AHORA);
    }

    @Test
    @DisplayName("Si una fila no cumple las reglas del campo no guarda nada y dice cuál campo está mal")
    void executeShouldExplainInvalidRows() {
        givenReclamado(givenTexto("sí"));
        PropuestaWhatsapp propuesta = givenPendiente(List.of(
                new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "mucho"), Set.of())));
        givenTablaGuardable(0);

        atender.execute();

        verify(registroRepository, never()).saveAll(any());
        assertThat(thenRespuesta()).contains("No guardé nada", "Monto (tiene que ser un número)");
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
    }

    @Test
    @DisplayName("Si el plan no admite más registros responde la restricción y no guarda")
    void executeShouldRespectTheRecordLimit() {
        givenReclamado(givenTexto("sí"));
        givenPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())));
        givenTablaGuardable(0);
        doThrow(new RestriccionPlanException("LIMITE_REGISTROS", "Llegaste al límite de registros de tu plan"))
                .when(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);

        atender.execute();

        verify(registroRepository, never()).saveAll(any());
        verify(mensajeria).enviarTexto(TELEFONO, "Llegaste al límite de registros de tu plan");
    }

    @Test
    @DisplayName("Si la tabla ya no existe al confirmar lo avisa y no guarda")
    void executeShouldExplainWhenTheTableWasDeleted() {
        givenReclamado(givenTexto("sí"));
        givenPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())));
        when(bloqueRepository.findByIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.TABLA_INEXISTENTE);
        verify(registroRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Responder no descarta la propuesta sin guardar nada")
    void executeShouldDiscardWhenUserAnswersNo() {
        givenReclamado(givenTexto("NO"));
        PropuestaWhatsapp propuesta = givenPendiente(List.of());

        atender.execute();

        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTADO);
        verify(registroRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Un mensaje nuevo mientras hay una propuesta abierta la descarta, lo avisa y atiende el mensaje")
    void executeShouldDiscardPreviousProposalOnNewMessage() {
        givenReclamado(givenTexto("vendí 3 tortas"));
        PropuestaWhatsapp anterior = givenPendiente(List.of());
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(anterior.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
        verify(interpreteIa).interpretar(any());
    }

    @Test
    @DisplayName("Responder sí a una propuesta vencida avisa que venció y no guarda")
    void executeShouldExplainExpiredProposal() {
        givenReclamado(givenTexto("sí"));
        PropuestaWhatsapp vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));

        atender.execute();

        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.VENCIDA);
        verify(registroRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Un texto nuevo con una propuesta vencida la cierra y se atiende como mensaje nuevo")
    void executeShouldIgnoreExpiredProposalOnNewText() {
        givenReclamado(givenTexto("gasté 5000 en harina"));
        PropuestaWhatsapp vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
        verify(mensajeria, never()).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
        verify(interpreteIa).interpretar(any());
    }

    @Test
    @DisplayName("Elegir la tabla por su número vuelve a interpretar el texto con esa tabla y queda pendiente")
    void executeShouldResolveTableChoiceForText() {
        givenReclamado(givenTexto("2"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.TEXTO, "5000 de harina", null, List.of(VENTAS_ID, GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        ArgumentCaptor<com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion> consulta =
                ArgumentCaptor.forClass(com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion.class);
        verify(interpreteIa).interpretar(consulta.capture());
        assertThat(consulta.getValue().destino()).isEqualTo(GASTOS_ID);
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(thenRespuesta()).contains("Para *Gastos*");
    }

    @Test
    @DisplayName("Elegir un número que no está en la lista lo pide de nuevo y mantiene la propuesta")
    void executeShouldRejectNumberOutsideTheList() {
        givenReclamado(givenTexto("7"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(VENTAS_ID, GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, "Elegí un número de la lista");
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
    }

    // --- fotos ---

    @Test
    @DisplayName("Una foto cuya estructura coincide con una tabla arma la propuesta y marca lo dudoso con (?)")
    void executeShouldProposeRowsFromPhoto() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                        "monto", new DatoLeido("5000", 0.3)))));

        atender.execute();

        verify(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.LEYENDO_FOTO);
        PropuestaWhatsapp propuesta = thenPropuestaGuardada();
        assertThat(propuesta.getOrigen()).isEqualTo(OrigenPropuestaWhatsapp.FOTO);
        assertThat(propuesta.getFilas().getFirst().dudosos()).containsExactly(MONTO_ID.toString());
        assertThat(thenRespuesta()).contains("Monto: 5000 (?)", "(?) = dato que no pude leer bien.");
    }

    @Test
    @DisplayName("Una foto que no se parece a ninguna tabla ofrece la lista y guarda la lectura para no leerla de nuevo")
    void executeShouldAskTableForAmbiguousPhoto() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Hoja",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));

        atender.execute();

        PropuestaWhatsapp propuesta = thenPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
        assertThat(propuesta.getLectura()).isNotNull();
        assertThat(propuesta.getOpciones()).containsExactly(GASTOS_ID, VENTAS_ID);
    }

    @Test
    @DisplayName("Elegir la tabla de una foto la asigna sin volver a leerla ni cobrar otra página")
    void executeShouldRemapPhotoWithoutReadingAgain() {
        givenReclamado(givenTexto("1"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.FOTO, null, lectura("Hoja", new FilaLeida(1, 1, Map.of("concepto",
                        new DatoLeido("harina", 0.9), "monto", new DatoLeido("5000", 0.9)))),
                List.of(GASTOS_ID, VENTAS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));

        atender.execute();

        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(eligiendo.getBloqueId()).isEqualTo(GASTOS_ID);
        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
    }

    @Test
    @DisplayName("Si el pie de la foto nombra una tabla, la lectura se hace contra esa tabla")
    void executeShouldUseTableNamedInCaption() {
        givenReclamado(givenFoto("las ventas de hoy"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Ventas",
                new FilaLeida(1, 1, Map.of("producto", new DatoLeido("torta", 0.9)))));

        atender.execute();

        verify(lectorCuadernoPort).leer(any(), any(), eq(VENTAS_ID));
        assertThat(thenPropuestaGuardada().getBloqueId()).isEqualTo(VENTAS_ID);
    }

    @Test
    @DisplayName("Si la foto no se puede descargar no se cobra la página y se pide mandarla de nuevo")
    void executeShouldNotChargeWhenDownloadFails() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenThrow(new IllegalStateException("sin archivo"));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.FOTO_NO_DESCARGADA);
        verifyNoInteractions(controlPlanService, lectorCuadernoPort);
    }

    @Test
    @DisplayName("Con el cupo mensual de fotos agotado responde la restricción y no llama a la IA")
    void executeShouldRespectThePhotoQuota() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Llegaste al límite de páginas de IA de tu plan"))
                .when(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, "Llegaste al límite de páginas de IA de tu plan");
        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("Una foto que no es JPG, PNG ni WEBP se rechaza antes de cobrar")
    void executeShouldRejectUnreadableImage() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(new byte[]{1, 2, 3}, "image/gif"));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, "Elegí una imagen JPG, PNG o WEBP");
        verifyNoInteractions(controlPlanService);
    }

    // --- ramas restantes ---

    @Test
    @DisplayName("Una foto con una propuesta abierta la descarta y lo avisa antes de leer la foto nueva")
    void executeShouldDiscardOpenProposalOnPhoto() {
        givenReclamado(givenFoto(null));
        PropuestaWhatsapp anterior = givenPendiente(List.of());
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));

        atender.execute();

        assertThat(anterior.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
    }

    @Test
    @DisplayName("Una nota de voz con una propuesta vencida no se toma como respuesta")
    void executeShouldTreatAudioAsNewMessageWhenProposalExpired() {
        givenReclamado(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("A2")
                .tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").recibidoEn(AHORA).build());
        PropuestaWhatsapp vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
    }

    @Test
    @DisplayName("Un número mientras hay una carga pendiente es un mensaje nuevo, no la elección de una tabla")
    void executeShouldTreatNumberAsNewTextWhenProposalIsPending() {
        givenReclamado(givenTexto("2"));
        PropuestaWhatsapp pendiente = givenPendiente(List.of());
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(pendiente.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(interpreteIa).interpretar(any());
    }

    @Test
    @DisplayName("Una tabla sin filas o con una fila sin datos cargables pide aclaración")
    void executeShouldAskClarificationWhenNoUsableRows() {
        givenReclamado(givenTexto("algo"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID, List.of(), 0.9, null));
        atender.execute();
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(givenTexto("otra")));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(PRODUCTO_AJENO_ID, "x", CONCEPTO_ID, " ")), 0.9, null));

        atender.execute();

        verify(mensajeria, org.mockito.Mockito.times(2)).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Una foto cuyas filas no traen datos cargables pide aclaración en lugar de proponer")
    void executeShouldAskClarificationWhenPhotoHasNoUsableRows() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido(" ", 0.9), "monto", new DatoLeido(null, 0.9)))));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
    }

    @Test
    @DisplayName("Un pie que nombra dos tablas o está en blanco no elige ninguna")
    void executeShouldIgnoreAmbiguousOrBlankCaption() {
        givenReclamado(givenFoto("gastos y ventas"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));
        atender.execute();
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(givenFoto("   ")));
        atender.execute();

        verify(lectorCuadernoPort, org.mockito.Mockito.times(2)).leer(any(), any(),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    @DisplayName("Elegir una tabla que ya no existe lo avisa")
    void executeShouldExplainWhenChosenTableNoLongerExists() {
        givenReclamado(givenTexto("1"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(BloqueId.nuevo()), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));

        atender.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.TABLA_INEXISTENTE);
    }

    @Test
    @DisplayName("Elegir una tabla cuyo texto no deja filas descarta la propuesta y pide aclaración")
    void executeShouldDiscardWhenChosenTableYieldsNoRows() {
        givenReclamado(givenTexto("1"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        when(interpreteIa.interpretar(any())).thenReturn(InterpretacionRegistros.sinResultado(null));

        atender.execute();

        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
    }

    @Test
    @DisplayName("Las columnas calculadas o conectadas no se cargan por WhatsApp")
    void executeShouldSkipFormulaAndRelationColumns() {
        TablaDisponible conCalculadas = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
                new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                new CampoConocido(MONTO_ID, "Total", TipoCampo.FORMULA, List.of()),
                new CampoConocido(PRODUCTO_ID, "Cliente", TipoCampo.RELACION, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(conCalculadas));
        givenReclamado(givenTexto("gasté 5000 en harina"));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    @Test
    @DisplayName("En una foto las columnas nuevas sin mapear se ignoran")
    void executeShouldIgnoreUnmappedColumnsInPhoto() {
        givenReclamado(givenFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                        "extra", new DatoLeido("algo", 0.9)))));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    @Test
    @DisplayName("Un valor nulo en la respuesta de la IA se descarta")
    void executeShouldDropNullValuesFromTheInterpretation() {
        givenReclamado(givenTexto("gasté 5000 en harina"));
        Map<CampoId, String> fila = new java.util.HashMap<>();
        fila.put(CONCEPTO_ID, "harina");
        fila.put(MONTO_ID, null);
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID, List.of(fila), 0.9,
                consumo()));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    @Test
    @DisplayName("En una foto una columna que corresponde a un campo calculado no se carga")
    void executeShouldNotLoadPhotoColumnsMappedToFormulaFields() {
        TablaDisponible conFormula = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
                new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                new CampoConocido(MONTO_ID, "Total", TipoCampo.FORMULA, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(conFormula));
        givenReclamado(givenFoto("gastos"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(
                TipoRegistroCuaderno.OTRO, new BloquePropuesto("Gastos", null, List.of(
                        new CampoPropuesto("concepto", "Concepto", TipoCampo.TEXTO, List.of()),
                        new CampoPropuesto("total", "Total", TipoCampo.NUMERO, List.of()))),
                List.of(new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                        "total", new DatoLeido("5000", 0.9)))), List.of(),
                new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30)));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    // --- actualizar filas existentes ---

    @Test
    @DisplayName("Si el producto ya existe, aunque lo escriban a medias, propone actualizarlo sin tocar su nombre")
    void executeShouldProposeUpdatingTheExistingRow() {
        givenFilaExistente("Harina 000", "1000");
        givenReclamado(givenTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));

        atender.execute();

        FilaPropuesta fila = thenPropuestaGuardada().getFilas().getFirst();
        assertThat(fila.actualizaUnaFila()).isTrue();
        assertThat(fila.registroId()).isEqualTo(REGISTRO_ID.toString());
        assertThat(fila.nombreExistente()).isEqualTo("Harina 000");
        assertThat(thenRespuesta()).contains("1) Actualizo *Harina 000*: Monto: 5000").doesNotContain("Concepto");
    }

    @Test
    @DisplayName("Confirmar una actualización cambia sólo los datos que vinieron y no agrega una fila nueva")
    void executeShouldUpdateTheExistingRowOnConfirm() {
        givenReclamado(givenTexto("sí"));
        Registro existente = givenFilaExistente("Harina 000", "1000");
        givenPendiente(List.of(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina",
                MONTO_ID.toString(), "5000"), Set.of()).actualizando(REGISTRO_ID.toString(), "Harina 000",
                CONCEPTO_ID.toString())));
        givenTablaGuardable(3);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));

        atender.execute();

        assertThat(existente.getValores()).containsEntry(CONCEPTO_ID.toString(), "Harina 000")
                .containsEntry(MONTO_ID.toString(), new java.math.BigDecimal("5000"));
        assertThat(existente.getActualizadoEn()).isEqualTo(AHORA);
        verify(registroRepository).saveAll(List.of(existente));
        verify(controlPlanService, never()).validarAltaRegistros(any(), org.mockito.ArgumentMatchers.anyLong(), any());
        assertThat(thenRespuesta()).contains("actualicé 1 fila en *Gastos*");
    }

    @Test
    @DisplayName("Una confirmación con filas nuevas y actualizadas hace las dos cosas y descuenta sólo las nuevas")
    void executeShouldMixUpdatesAndAdditions() {
        givenReclamado(givenTexto("sí"));
        Registro existente = givenFilaExistente("Harina 000", "1000");
        givenPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())
                        .actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString()),
                new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "azúcar", MONTO_ID.toString(), "800"), Set.of())));
        givenTablaGuardable(3);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));

        atender.execute();

        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        assertThat(thenRespuesta()).contains("actualicé 1 y agregué 1 filas en *Gastos*");
    }

    @Test
    @DisplayName("Si la fila a actualizar se borró o es de otra tabla, el dato se agrega como fila nueva")
    void executeShouldAddAsNewWhenTheExistingRowIsGone() {
        givenReclamado(givenTexto("sí"));
        Registro deOtraTabla = Registro.nuevo(NEGOCIO_ID, VENTAS_ID, Map.of(), AHORA);
        givenPendiente(List.of(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "5000"),
                Set.of()).actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString())));
        givenTablaGuardable(0);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.empty(),
                Optional.of(deOtraTabla));

        atender.execute();

        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        assertThat(thenRespuesta()).contains("guardé 1 fila en *Gastos*");
    }

    @Test
    @DisplayName("Una actualización cuyo resultado no cumple las reglas del campo no guarda nada")
    void executeShouldValidateTheMergedRow() {
        givenReclamado(givenTexto("sí"));
        Registro existente = givenFilaExistente("Harina 000", "1000");
        givenPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "mucho"), Set.of())
                .actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString())));
        givenTablaGuardable(0);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));

        atender.execute();

        verify(registroRepository, never()).saveAll(any());
        assertThat(thenRespuesta()).contains("No guardé nada", "Monto");
    }

    @Test
    @DisplayName("Una tabla de movimientos (gastos) suma una fila nueva aunque ya haya otra con el mismo nombre")
    void executeShouldNotUpdateInMovementTables() {
        TablaDisponible movimientos = new TablaDisponible(gastos.bloque(), "Mi panadería",
                com.ensambla.backend.domain.model.bloque.TipoBloque.GASTOS);
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(movimientos));
        givenFilaExistente("harina", "1000");
        givenReclamado(givenTexto("gasté 5000 en harina"));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().actualizaUnaFila()).isFalse();
        assertThat(thenRespuesta()).contains("1) Agrego:");
    }

    @Test
    @DisplayName("Si no hay un producto parecido, o hay dos igual de parecidos, se agrega una fila nueva")
    void executeShouldAddWhenNoClearMatch() {
        givenFilaExistente("Harina 000", "1000");
        givenFilaExistente("Harina 0000", "1200");
        givenReclamado(givenTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000"), Map.of(CONCEPTO_ID, "manteca",
                        MONTO_ID, "900")), 0.9, consumo()));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas()).noneMatch(FilaPropuesta::actualizaUnaFila);
    }

    @Test
    @DisplayName("Un dato sin nombre de producto, o sólo con el nombre, queda como fila nueva")
    void executeShouldKeepRowsWithoutNameOrWithOnlyTheName() {
        givenFilaExistente("Harina 000", "1000");
        givenReclamado(givenTexto("cosas"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(MONTO_ID, "700"), Map.of(CONCEPTO_ID, "harina")), 0.9, consumo()));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas()).noneMatch(FilaPropuesta::actualizaUnaFila);
    }

    @Test
    @DisplayName("Una tabla sin columna de texto no tiene cómo nombrar sus filas y siempre agrega")
    void executeShouldAddWhenTheTableHasNoTextColumn() {
        TablaDisponible sinTexto = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Solo montos", List.of(
                new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(sinTexto));
        givenReclamado(givenTexto("5000"));
        givenInterpretacion(GASTOS_ID, 0.9);

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().actualizaUnaFila()).isFalse();
    }

    @Test
    @DisplayName("En una foto también actualiza el producto existente en lugar de sumar uno nuevo")
    void executeShouldUpdateExistingRowsFromPhotos() {
        givenFilaExistente("Harina 000", "1000");
        givenReclamado(givenFoto("gastos"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                        "monto", new DatoLeido("5000", 0.9)))));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().nombreExistente()).isEqualTo("Harina 000");
    }

    @Test
    @DisplayName("Al elegir la tabla de un texto también empareja con las filas existentes")
    void executeShouldMatchExistingRowsWhenChoosingTheTable() {
        givenFilaExistente("Harina 000", "1000");
        givenReclamado(givenTexto("1"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                OrigenPropuestaWhatsapp.TEXTO, "harina 5000", null, List.of(GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));

        atender.execute();

        assertThat(eligiendo.getFilas().getFirst().actualizaUnaFila()).isTrue();
    }

    @Test
    @DisplayName("Las filas existentes sin nombre se ignoran al buscar el producto")
    void executeShouldIgnoreExistingRowsWithoutAName() {
        Registro sinNombre = Registro.nuevo(NEGOCIO_ID, GASTOS_ID, Map.of(MONTO_ID.toString(), 10), AHORA);
        sinNombre.setId(RegistroId.nuevo());
        existentes.add(sinNombre);
        givenFilaExistente("Harina 000", "1000");
        givenReclamado(givenTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));

        atender.execute();

        assertThat(thenPropuestaGuardada().getFilas().getFirst().nombreExistente()).isEqualTo("Harina 000");
    }

    private Registro givenFilaExistente(String nombre, String monto) {
        Registro registro = Registro.nuevo(NEGOCIO_ID, GASTOS_ID, new java.util.HashMap<>(Map.of(
                CONCEPTO_ID.toString(), nombre, MONTO_ID.toString(), new java.math.BigDecimal(monto))), AHORA);
        registro.setId(existentes.isEmpty() ? REGISTRO_ID : RegistroId.nuevo());
        existentes.add(registro);
        when(registroRepository.findAllByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(existentes);
        return registro;
    }

    // --- arrange / assert ---

    private MensajeWhatsapp givenTexto(String texto) {
        return MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("T1")
                .tipo(TipoMensajeWhatsapp.TEXTO).texto(texto).recibidoEn(AHORA).build();
    }

    private MensajeWhatsapp givenFoto(String pie) {
        return MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("F1")
                .tipo(TipoMensajeWhatsapp.IMAGEN).texto(pie).mimeMedia("image/jpeg").recibidoEn(AHORA).build();
    }

    private MensajeWhatsapp givenReclamado(MensajeWhatsapp mensaje) {
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    private void givenInterpretacion(BloqueId bloqueId, double confianza) {
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(bloqueId,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), confianza, consumo()));
    }

    private PropuestaWhatsapp givenPendiente(List<FilaPropuesta> filas) {
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                "gasté", GASTOS_ID, filas, AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(propuesta));
        return propuesta;
    }

    private void givenTablaGuardable(int ordenSiguiente) {
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(NEGOCIO_ID).nombre("Gastos").build();
        when(bloqueRepository.findByIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(Optional.of(bloque));
        when(campoRepository.findByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(CONCEPTO_ID).nombre("Concepto").tipo(TipoCampo.TEXTO).posicion((short) 0).build(),
                Campo.builder().id(MONTO_ID).nombre("Monto").tipo(TipoCampo.MONEDA).posicion((short) 1).build()));
        when(registroRepository.siguienteOrden(GASTOS_ID, NEGOCIO_ID)).thenReturn(ordenSiguiente);
    }

    private static LecturaCuaderno lectura(String nombreTabla, FilaLeida... filas) {
        List<CampoPropuesto> campos = nombreTabla.equals("Ventas")
                ? List.of(new CampoPropuesto("producto", "Producto", TipoCampo.TEXTO, List.of()))
                : List.of(new CampoPropuesto("concepto", "Concepto", TipoCampo.TEXTO, List.of()),
                        new CampoPropuesto("monto", "Monto", TipoCampo.MONEDA, List.of()));
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto(nombreTabla, null, campos),
                List.of(filas), List.of(), new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30));
    }

    private static ConsumoIaSami consumo() {
        return new ConsumoIaSami("simulado", "demo", 10, 20);
    }

    private PropuestaWhatsapp thenPropuestaGuardada() {
        ArgumentCaptor<PropuestaWhatsapp> captor = ArgumentCaptor.forClass(PropuestaWhatsapp.class);
        verify(propuestaRepository).save(captor.capture());
        return captor.getValue();
    }

    private String thenRespuesta() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mensajeria, org.mockito.Mockito.atLeastOnce()).enviarTexto(eq(TELEFONO), captor.capture());
        return String.join("\n", captor.getAllValues());
    }
}
