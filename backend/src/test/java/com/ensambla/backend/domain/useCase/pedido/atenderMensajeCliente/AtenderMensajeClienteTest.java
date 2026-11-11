package com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.EstadoConversacionCliente;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.LineaPedida;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.pedido.ProductoFaltante;
import com.ensambla.backend.domain.model.pedido.TextosPedido;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.port.InterpretePedidoIaPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.ComercioRepository;
import com.ensambla.backend.domain.repository.pedido.ConversacionClienteRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.support.PedidoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PedidoTestData.INVENTARIO_ID;
import static com.ensambla.backend.support.PedidoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PedidoTestData.proyectoConInventario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtenderMensajeClienteTest {

    private static final Instant INSTANTE = Instant.parse("2026-11-11T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final Duration VIGENCIA = Duration.ofMinutes(2);
    private static final String TELEFONO = "5491199990000";
    private static final ClienteId CLIENTE_ID = ClienteId.de("c0000000-0000-4000-8000-000000000001");
    private static final Comercio DON_PEPE = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
            "Almacén Don Pepe");
    private static final Comercio DONA_ROSA = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000002"),
            "Almacén Doña Rosa");
    private static final Comercio KIOSCO = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000003"),
            "Kiosco Sol");
    private static final int LIMITE_SAMI = 60;
    private static final NegocioId CERRADO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000009");

    private final MensajeClienteRepository mensajeRepository = mock(MensajeClienteRepository.class);
    private final ClienteWhatsappRepository clienteRepository = mock(ClienteWhatsappRepository.class);
    private final ConversacionClienteRepository conversacionRepository = mock(ConversacionClienteRepository.class);
    private final ComercioRepository comercioRepository = mock(ComercioRepository.class);
    private final OrdenCompraRepository ordenRepository = mock(OrdenCompraRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final InterpretePedidoIaPort interpreteIa = mock(InterpretePedidoIaPort.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeCliente atenderMensajeCliente = new AtenderMensajeCliente(mensajeRepository,
            clienteRepository, conversacionRepository, comercioRepository, ordenRepository, proyectoRepository,
            new DatosDeProyectoService(registroRepository), controlPlanService, consumoSamiRepository, interpreteIa,
            mensajeria, new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC), VIGENCIA, LIMITE_SAMI);

    // --- la cola ---

    @Test
    @DisplayName("Sin mensajes en la cola no hace nada y avisa que no había")
    void executeShouldReturnFalseWhenQueueIsEmpty() {
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of());
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.empty());

        boolean habia = atenderMensajeCliente.execute();

        assertThat(habia).isFalse();
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("Un mensaje reclamado que nunca terminó se da por perdido, se avisa al cliente y no se reintenta")
    void executeShouldFailLostMessages() {
        MensajeCliente perdido = givenMensaje("hola");
        perdido.reclamar(AHORA.minusMinutes(10), VIGENCIA);
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of(perdido));
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.empty());

        atenderMensajeCliente.execute();

        assertThat(perdido.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(perdido.getError()).isEqualTo("Se perdió mientras se atendía");
        verify(mensajeRepository).save(perdido);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
    }

    @Test
    @DisplayName("Una regla de negocio rota se le dice al cliente y el mensaje queda atendido")
    void executeShouldReplyBusinessRuleMessage() {
        MensajeCliente mensaje = givenMensajeReclamado("hola");
        doThrow(new BusinessRuleException("No se puede")).when(clienteRepository).findByTelefono(TELEFONO);

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, "No se puede");
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.ATENDIDO);
    }

    @Test
    @DisplayName("Un error inesperado le pide al cliente que pruebe de nuevo y deja el mensaje fallido con el motivo")
    void executeShouldFailTheMessageOnUnexpectedError() {
        MensajeCliente mensaje = givenMensajeReclamado("hola");
        doThrow(new IllegalStateException("Base no disponible")).when(clienteRepository).findByTelefono(TELEFONO);

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).isEqualTo("IllegalStateException: Base no disponible");
    }

    // --- empezar ---

    @Test
    @DisplayName("El primer mensaje de un cliente nuevo lo registra, lo saluda y le pregunta el comercio")
    void executeShouldRegisterNewClientAndAskForTheBusiness() {
        MensajeCliente mensaje = givenMensajeReclamado("hola");
        givenComercios(DON_PEPE, KIOSCO);
        when(clienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(ClienteWhatsapp.class))).thenReturn(givenCliente(null));
        givenSinConversacion();

        boolean habia = atenderMensajeCliente.execute();

        assertThat(habia).isTrue();
        ArgumentCaptor<ClienteWhatsapp> captor = ArgumentCaptor.forClass(ClienteWhatsapp.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getTelefono()).isEqualTo(TELEFONO);
        assertThat(captor.getValue().getCreadoEn()).isEqualTo(AHORA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.BIENVENIDA);
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getNegocioId()).isNull();
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.ATENDIDO);
    }

    @Test
    @DisplayName("Un cliente que ya pidió antes recibe la oferta de seguir con el último comercio")
    void executeShouldOfferTheLastBusiness() {
        givenMensajeReclamado("hola");
        givenComercios(DON_PEPE, KIOSCO);
        givenClienteConocido(DON_PEPE.id());
        givenSinConversacion();

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.bienvenidaConUltimo("Almacén Don Pepe"));
        assertThat(thenConversacionGuardada().getNegocioId()).isEqualTo(DON_PEPE.id());
    }

    @Test
    @DisplayName("Si el último comercio ya no recibe pedidos no se lo ofrece")
    void executeShouldNotOfferAClosedBusiness() {
        givenMensajeReclamado("hola");
        givenComercios(KIOSCO);
        givenClienteConocido(CERRADO_ID);
        givenSinConversacion();

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.BIENVENIDA);
        assertThat(thenConversacionGuardada().getNegocioId()).isNull();
    }

    @Test
    @DisplayName("Si pasó mucho tiempo desde la última vez, saluda de nuevo en lugar de seguir donde estaba")
    void executeShouldGreetAgainWhenTheConversationExpired() {
        givenMensajeReclamado("dame yerba");
        givenComercios(DON_PEPE);
        givenClienteConocido(DON_PEPE.id());
        givenConversacion(EstadoConversacionCliente.PIDIENDO, DON_PEPE.id(), List.of(),
                AHORA.minus(ConversacionCliente.VIGENCIA).minusMinutes(1));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.bienvenidaConUltimo("Almacén Don Pepe"));
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA);
    }

    // --- elegir el comercio ---

    @Test
    @DisplayName("Responder SÍ a la oferta sigue con el último comercio")
    void executeShouldContinueWithTheLastBusinessOnYes() {
        givenMensajeReclamado("sí");
        givenComercios(DON_PEPE, KIOSCO);
        ClienteWhatsapp cliente = givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, DON_PEPE.id(), List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioElegido("Almacén Don Pepe"));
        thenPidiendoA(DON_PEPE);
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Responder SÍ cuando el último comercio ya no recibe pedidos vuelve a preguntar")
    void executeShouldAskAgainWhenTheOfferedBusinessIsGone() {
        givenMensajeReclamado("si");
        givenComercios(KIOSCO);
        givenClienteConocido(CERRADO_ID);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, CERRADO_ID, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.COMERCIO_NO_DISPONIBLE);
        assertThat(thenConversacionGuardada().getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Responder NO a la oferta pide el nombre de otro comercio y deja de ofrecer el último")
    void executeShouldAskForAnotherBusinessOnNo() {
        givenMensajeReclamado("no");
        givenComercios(DON_PEPE, KIOSCO);
        givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, DON_PEPE.id(), List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.PREGUNTAR_COMERCIO);
        assertThat(thenConversacionGuardada().getNegocioId()).isNull();
    }

    @Test
    @DisplayName("Un nombre que coincide con un solo comercio lo elige y lo recuerda como el último")
    void executeShouldPickTheBusinessByName() {
        givenMensajeReclamado("kiosco sol");
        givenComercios(DON_PEPE, DONA_ROSA, KIOSCO);
        ClienteWhatsapp cliente = givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioElegido("Kiosco Sol"));
        thenPidiendoA(KIOSCO);
        assertThat(cliente.getUltimoNegocioId()).isEqualTo(KIOSCO.id());
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Un nombre escrito a medias elige el comercio igual, sin tildes ni mayúsculas")
    void executeShouldTolerateSloppyNames() {
        givenMensajeReclamado("KIOSCO");
        givenComercios(DON_PEPE, DONA_ROSA, KIOSCO);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        thenPidiendoA(KIOSCO);
    }

    @Test
    @DisplayName("Si varios comercios se parecen los muestra numerados y espera que elija uno")
    void executeShouldListSimilarBusinesses() {
        givenMensajeReclamado("almacen");
        givenComercios(DON_PEPE, DONA_ROSA, KIOSCO);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.variosComercios(List.of(DON_PEPE, DONA_ROSA)));
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getOpciones()).containsExactly(DON_PEPE.id(), DONA_ROSA.id());
    }

    @Test
    @DisplayName("Un nombre que no coincide con ningún comercio se lo dice y sigue esperando")
    void executeShouldSayWhenNoBusinessMatches() {
        givenMensajeReclamado("panadería luna");
        givenComercios(DON_PEPE, KIOSCO);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioNoEncontrado("panadería luna"));
        assertThat(thenConversacionGuardada().getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un SÍ, un NO o un número sin nada que responder se toma como un nombre de comercio")
    void executeShouldTreatAnswersWithoutQuestionAsNames() {
        givenComercios(DON_PEPE);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());
        givenMensajeReclamado("sí");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioNoEncontrado("sí"));
    }

    @Test
    @DisplayName("Un número sin comercios para elegir se toma como un nombre")
    void executeShouldTreatNumberWithoutOptionsAsName() {
        givenMensajeReclamado("2");
        givenComercios(DON_PEPE);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioNoEncontrado("2"));
    }

    @Test
    @DisplayName("Un NO sin nada que rechazar se toma como un nombre")
    void executeShouldTreatNoWithoutQuestionAsName() {
        givenMensajeReclamado("no");
        givenComercios(DON_PEPE);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null, List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioNoEncontrado("no"));
    }

    @Test
    @DisplayName("Elegir con el número de una de las opciones sigue con ese comercio")
    void executeShouldPickTheOptionByNumber() {
        givenMensajeReclamado("2");
        givenComercios(DON_PEPE, DONA_ROSA, KIOSCO);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null,
                List.of(DON_PEPE.id(), DONA_ROSA.id()));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.comercioElegido("Almacén Doña Rosa"));
        thenPidiendoA(DONA_ROSA);
    }

    @Test
    @DisplayName("Un número que no corresponde a ninguna opción le pide que elija uno válido")
    void executeShouldRejectAnOptionOutOfRange() {
        givenMensajeReclamado("7");
        givenComercios(DON_PEPE, DONA_ROSA);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null,
                List.of(DON_PEPE.id(), DONA_ROSA.id()));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.opcionInvalida(2));
        assertThat(thenConversacionGuardada().getOpciones()).hasSize(2);
    }

    @Test
    @DisplayName("Si el comercio elegido de la lista ya no recibe pedidos vuelve a preguntar")
    void executeShouldAskAgainWhenTheChosenOptionIsGone() {
        givenMensajeReclamado("1");
        givenComercios(DONA_ROSA);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null,
                List.of(DON_PEPE.id(), DONA_ROSA.id()));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.COMERCIO_NO_DISPONIBLE);
        assertThat(thenConversacionGuardada().getOpciones()).isEmpty();
    }

    @Test
    @DisplayName("Responder NO a la lista de opciones pide otro nombre y la descarta")
    void executeShouldDiscardTheOptionsOnNo() {
        givenMensajeReclamado("no");
        givenComercios(DON_PEPE, DONA_ROSA);
        givenClienteConocido(null);
        givenConversacionVigente(EstadoConversacionCliente.ELIGIENDO_COMERCIO, null,
                List.of(DON_PEPE.id(), DONA_ROSA.id()));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.PREGUNTAR_COMERCIO);
        assertThat(thenConversacionGuardada().getOpciones()).isEmpty();
    }

    // --- pedir ---

    @Test
    @DisplayName("Arma la lista con productos reales del comercio, con precios y total, y la guarda en la conversación")
    void executeShouldBuildTheShoppingListFromTheStock() {
        givenMensajeReclamado("dame 2 yerba playadito, aceite y pañales");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10), registro("Aceite Cocinero 900 ml", 2000, 3),
                registro("Aceite Natura 900 ml", 2400, 8));
        ConsumoIaSami consumo = new ConsumoIaSami("bedrock", "haiku", 100, 20);
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(new PedidoInterpretado(List.of(
                new LineaPedida("yerba playadito", BigDecimal.valueOf(2)), new LineaPedida("aceite", null),
                new LineaPedida("pañales", null)), consumo));

        atenderMensajeCliente.execute();

        ArgumentCaptor<String> respuesta = ArgumentCaptor.forClass(String.class);
        verify(mensajeria).enviarTexto(org.mockito.ArgumentMatchers.eq(TELEFONO), respuesta.capture());
        assertThat(respuesta.getValue()).contains("Tu pedido en *Almacén Don Pepe*:",
                "1) Yerba Playadito 1 kg x2 — $7.000", "2) Aceite Cocinero 900 ml x1 — $2.000",
                "También tengo: Aceite Natura 900 ml", "Total: $9.000", "No tengo «pañales».");
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getLista()).extracting(linea -> linea.producto().nombre())
                .containsExactly("Yerba Playadito 1 kg", "Aceite Cocinero 900 ml");
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA);
        verify(controlPlanService).exigirIa(DON_PEPE.id());
        verify(consumoSamiRepository).registrar(DON_PEPE.id(), ZonaNegocio.dia(AHORA), consumo, AHORA);
    }

    @Test
    @DisplayName("A la IA le pasa lo que escribió el cliente y los nombres de los productos del comercio")
    void executeShouldGiveTheAiTheProductNames() {
        givenMensajeReclamado("dame yerba");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10), registro("Aceite Cocinero 900 ml", 2000, 3));
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(PedidoInterpretado.sinResultado(null));

        atenderMensajeCliente.execute();

        ArgumentCaptor<ConsultaPedido> consulta = ArgumentCaptor.forClass(ConsultaPedido.class);
        verify(interpreteIa).interpretar(consulta.capture());
        assertThat(consulta.getValue().texto()).isEqualTo("dame yerba");
        assertThat(consulta.getValue().productos()).containsExactly("Aceite Cocinero 900 ml", "Yerba Playadito 1 kg");
    }

    @Test
    @DisplayName("Una consulta sin costo (modo simulado) no se registra en el consumo")
    void executeShouldNotRecordConsumptionWhenThereIsNone() {
        givenMensajeReclamado("yerba");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10));
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(new PedidoInterpretado(
                List.of(new LineaPedida("yerba", null)), null));

        atenderMensajeCliente.execute();

        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
        assertThat(thenConversacionGuardada().getLista()).hasSize(1);
    }

    @Test
    @DisplayName("Un comercio sin productos con precio y stock no gasta una consulta de IA y lo dice")
    void executeShouldNotCallTheAiWhenThereAreNoProducts() {
        givenMensajeReclamado("dame yerba");
        givenPidiendoADonPepe();
        givenStock(registro("Agotado", 100, 0));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.sinProductos("Almacén Don Pepe"));
        verify(interpreteIa, never()).interpretar(any());
        verify(consumoSamiRepository, never()).reservar(any(), any(), anyInt(), any());
        assertThat(thenConversacionGuardada().getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Con el límite diario de Sami agotado avisa al cliente y no pregunta a la IA")
    void executeShouldRejectWhenTheDailyLimitIsReached() {
        MensajeCliente mensaje = givenMensajeReclamado("dame yerba");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10));
        when(consumoSamiRepository.reservar(DON_PEPE.id(), ZonaNegocio.dia(AHORA), LIMITE_SAMI, AHORA))
                .thenReturn(false);

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.LIMITE_SAMI);
        verify(interpreteIa, never()).interpretar(any());
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.ATENDIDO);
        verify(conversacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el mensaje no era un pedido le explica cómo pedir y no toca la lista")
    void executeShouldExplainHowToOrderWhenNothingWasUnderstood() {
        givenMensajeReclamado("gracias");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10));
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(PedidoInterpretado.sinResultado(null));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.NO_ENTENDI_PEDIDO);
        assertThat(thenConversacionGuardada().getLista()).isEmpty();
    }

    @Test
    @DisplayName("Si el comercio no tiene nada de lo pedido lo dice, recomienda lo parecido y no arma la lista")
    void executeShouldReportWhenNothingIsInStock() {
        givenMensajeReclamado("pañales");
        givenPidiendoADonPepe();
        givenStock(registro("Yerba Playadito 1 kg", 3500, 10));
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(new PedidoInterpretado(
                List.of(new LineaPedida("pañales", null)), null));

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO,
                TextosPedido.sinCoincidencias(List.of(new ProductoFaltante("pañales", null))));
        assertThat(thenConversacionGuardada().getLista()).isEmpty();
    }

    @Test
    @DisplayName("Escribir CAMBIAR mientras pide vuelve a preguntar el comercio")
    void executeShouldLetTheClientChangeTheBusiness() {
        givenMensajeReclamado("Cambiar");
        givenComercios(DON_PEPE, KIOSCO);
        givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.PIDIENDO, DON_PEPE.id(), List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.PREGUNTAR_COMERCIO);
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
        assertThat(conversacion.getNegocioId()).isNull();
    }

    @Test
    @DisplayName("Si el comercio al que le pedía ya no recibe pedidos, vuelve a preguntar")
    void executeShouldAskAgainWhenTheBusinessStoppedReceivingOrders() {
        givenMensajeReclamado("dame yerba");
        givenComercios(KIOSCO);
        givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.PIDIENDO, DON_PEPE.id(), List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.COMERCIO_NO_DISPONIBLE);
        assertThat(thenConversacionGuardada().getEstado()).isEqualTo(EstadoConversacionCliente.ELIGIENDO_COMERCIO);
    }

    // --- confirmar, descartar y cambiar de marca ---

    @Test
    @DisplayName("Con el SÍ del cliente la lista se convierte en una orden pendiente y la conversación queda libre")
    void executeShouldCreateThePendingOrderOnConfirmation() {
        Registro registro = registro("Yerba Playadito 1 kg", 3500, 10);
        ProductoEnStock yerba = givenProducto(registro);
        givenStock(registro);
        givenListaArmada(null, LineaDeCompra.de(yerba, BigDecimal.valueOf(2), List.of()));
        givenMensajeReclamado("sí");
        when(ordenRepository.save(any(OrdenCompra.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        atenderMensajeCliente.execute();

        ArgumentCaptor<OrdenCompra> orden = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenRepository).save(orden.capture());
        assertThat(orden.getValue().getNegocioId()).isEqualTo(DON_PEPE.id());
        assertThat(orden.getValue().getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(orden.getValue().getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
        assertThat(orden.getValue().getTotal()).isEqualByComparingTo("7000");
        assertThat(orden.getValue().getItems()).singleElement().satisfies(item -> {
            assertThat(item.productoId()).isEqualTo(yerba.id());
            assertThat(item.cantidad()).isEqualByComparingTo("2");
        });
        verify(mensajeria).enviarTexto(TELEFONO,
                TextosPedido.pedidoConfirmado("Almacén Don Pepe", new BigDecimal("7000")));
        assertThat(thenConversacionGuardada().getLista()).isEmpty();
    }

    @Test
    @DisplayName("Si entre tanto subió el precio o se acabó el stock no manda la orden y pide armar la lista de nuevo")
    void executeShouldNotCreateTheOrderWhenTheStockChanged() {
        Registro viejo = registro("Yerba Playadito 1 kg", 3500, 10);
        ProductoEnStock yerba = givenProducto(viejo);
        Registro masCara = PedidoTestData.registro("Yerba Playadito 1 kg", 4000, 10, false);
        ProductoEnStock mismoProductoMasCaro = givenProducto(masCara);
        givenStock(new Registro[]{masCara});
        givenListaArmada(null, LineaDeCompra.de(new ProductoEnStock(mismoProductoMasCaro.id(), yerba.nombre(),
                yerba.precio(), yerba.stock()), BigDecimal.valueOf(2), List.of()));
        givenMensajeReclamado("confirmo");

        atenderMensajeCliente.execute();

        verify(ordenRepository, never()).save(any());
        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.cambioDeStock(List.of("Yerba Playadito 1 kg")));
        assertThat(thenConversacionGuardada().getLista()).isEmpty();
    }

    @Test
    @DisplayName("Si el producto se quedó sin stock suficiente tampoco manda la orden")
    void executeShouldNotCreateTheOrderWhenThereIsNoStockLeft() {
        Registro registro = registro("Yerba Playadito 1 kg", 3500, 1);
        ProductoEnStock yerba = givenProducto(registro);
        givenStock(registro);
        givenListaArmada(null, new LineaDeCompra(yerba, BigDecimal.valueOf(2), false, List.of()));
        givenMensajeReclamado("ok");

        atenderMensajeCliente.execute();

        verify(ordenRepository, never()).save(any());
        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.cambioDeStock(List.of("Yerba Playadito 1 kg")));
    }

    @Test
    @DisplayName("Con el NO del cliente descarta la lista sin crear ninguna orden")
    void executeShouldDiscardTheListOnNo() {
        givenListaArmada(null, LineaDeCompra.de(givenProducto(registro("Yerba", 3500, 10)), null, List.of()));
        givenMensajeReclamado("no");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.PEDIDO_DESCARTADO);
        verify(ordenRepository, never()).save(any());
        assertThat(thenConversacionGuardada().getLista()).isEmpty();
    }

    @Test
    @DisplayName("Un SÍ o un NO sin lista armada no gasta una consulta de IA y explica cómo pedir")
    void executeShouldNotCallTheAiForAnswersWithoutAList() {
        givenPidiendoADonPepe();
        givenMensajeReclamado("sí");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.NO_ENTENDI_PEDIDO);
        verify(interpreteIa, never()).interpretar(any());
        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un NO sin lista armada también explica cómo pedir")
    void executeShouldExplainHowToOrderOnNoWithoutAList() {
        givenPidiendoADonPepe();
        givenMensajeReclamado("no");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.NO_ENTENDI_PEDIDO);
        verify(interpreteIa, never()).interpretar(any());
    }

    @Test
    @DisplayName("CAMBIAR con el número de un renglón muestra las otras marcas y espera que elija una")
    void executeShouldOfferTheOtherBrandsOfALine() {
        LineaDeCompra aceite = givenAceiteConAlternativa();
        givenListaArmada(null, givenYerba(), aceite);
        givenMensajeReclamado("cambiar 2");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.alternativas(aceite));
        assertThat(thenConversacionGuardada().getRenglonACambiar()).isEqualTo(2);
    }

    @Test
    @DisplayName("CAMBIAR con el nombre del producto también encuentra el renglón")
    void executeShouldFindTheLineByName() {
        LineaDeCompra aceite = givenAceiteConAlternativa();
        givenListaArmada(null, givenYerba(), aceite);
        givenMensajeReclamado("cambiar el aceite");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.alternativas(aceite));
        assertThat(thenConversacionGuardada().getRenglonACambiar()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un renglón sin otras marcas lo dice y no espera ninguna elección")
    void executeShouldSayWhenALineHasNoAlternatives() {
        givenListaArmada(null, givenYerba());
        givenMensajeReclamado("cambiar 1");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.sinAlternativas("Yerba Playadito 1 kg"));
        assertThat(thenConversacionGuardada().getRenglonACambiar()).isNull();
    }

    @Test
    @DisplayName("CAMBIAR con un renglón que no existe explica cómo pedirlo")
    void executeShouldRejectAnUnknownLine() {
        givenListaArmada(null, givenYerba());
        givenMensajeReclamado("cambiar 9");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.RENGLON_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("Elegir una de las otras marcas con su número cambia ese renglón y muestra la lista de nuevo")
    void executeShouldSwitchTheBrandWithTheChosenNumber() {
        LineaDeCompra aceite = givenAceiteConAlternativa();
        givenListaArmada(2, givenYerba(), aceite);
        givenMensajeReclamado("1");

        atenderMensajeCliente.execute();

        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getLista().get(1).producto().nombre()).isEqualTo("Aceite Natura 900 ml");
        assertThat(conversacion.getRenglonACambiar()).isNull();
        ArgumentCaptor<String> respuesta = ArgumentCaptor.forClass(String.class);
        verify(mensajeria).enviarTexto(org.mockito.ArgumentMatchers.eq(TELEFONO), respuesta.capture());
        assertThat(respuesta.getValue()).contains("2) Aceite Natura 900 ml x1 — $2.400", "Total: $5.900");
    }

    @Test
    @DisplayName("Un número fuera de las otras marcas pide elegir uno válido y sigue esperando")
    void executeShouldRejectAnInvalidBrandNumber() {
        givenListaArmada(2, givenYerba(), givenAceiteConAlternativa());
        givenMensajeReclamado("5");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.opcionDeMarcaInvalida(1));
        assertThat(thenConversacionGuardada().getRenglonACambiar()).isEqualTo(2);
    }

    @Test
    @DisplayName("El número cero tampoco es una de las otras marcas")
    void executeShouldRejectZeroAsABrandNumber() {
        givenListaArmada(2, givenYerba(), givenAceiteConAlternativa());
        givenMensajeReclamado("0");

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.opcionDeMarcaInvalida(1));
    }

    @Test
    @DisplayName("Responder NO al cambio de marca deja la lista como estaba y la muestra de nuevo")
    void executeShouldKeepTheBrandOnNo() {
        LineaDeCompra aceite = givenAceiteConAlternativa();
        givenListaArmada(2, givenYerba(), aceite);
        givenMensajeReclamado("no");

        atenderMensajeCliente.execute();

        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getLista().get(1)).isEqualTo(aceite);
        assertThat(conversacion.getRenglonACambiar()).isNull();
        verify(mensajeria).enviarTexto(org.mockito.ArgumentMatchers.eq(TELEFONO), anyString());
    }

    @Test
    @DisplayName("Mientras espera una marca, un mensaje que no es un número ni NO se toma como un pedido nuevo")
    void executeShouldTreatOtherTextAsANewOrderWhileChoosingABrand() {
        Registro fideos = registro("Fideos Matarazzo 500 g", 1200, 20);
        givenStock(fideos);
        givenListaArmada(2, givenYerba(), givenAceiteConAlternativa());
        givenMensajeReclamado("dame fideos");
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(new PedidoInterpretado(
                List.of(new LineaPedida("fideos", null)), null));

        atenderMensajeCliente.execute();

        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getLista()).extracting(linea -> linea.producto().nombre())
                .containsExactly("Fideos Matarazzo 500 g");
        assertThat(conversacion.getRenglonACambiar()).isNull();
    }

    @Test
    @DisplayName("Con la lista armada, un mensaje que no es SÍ, NO ni cambiar es un pedido nuevo que la reemplaza")
    void executeShouldReplaceTheListWithANewOrder() {
        givenStock(registro("Fideos Matarazzo 500 g", 1200, 20));
        givenListaArmada(null, givenYerba());
        givenMensajeReclamado("mejor dame fideos");
        when(interpreteIa.interpretar(any(ConsultaPedido.class))).thenReturn(new PedidoInterpretado(
                List.of(new LineaPedida("fideos", null)), null));

        atenderMensajeCliente.execute();

        assertThat(thenConversacionGuardada().getLista()).extracting(linea -> linea.producto().nombre())
                .containsExactly("Fideos Matarazzo 500 g");
    }

    // --- datos de prueba ---

    private static ProductoEnStock givenProducto(Registro registro) {
        return ProductoEnStock.de(PedidoTestData.inventario(registro), registro).orElseThrow();
    }

    private static LineaDeCompra givenYerba() {
        return LineaDeCompra.de(givenProducto(registro("Yerba Playadito 1 kg", 3500, 10)), null, List.of());
    }

    /** Un aceite de 2000 con otra marca de 2400 como alternativa. */
    private static LineaDeCompra givenAceiteConAlternativa() {
        return LineaDeCompra.de(givenProducto(registro("Aceite Cocinero 900 ml", 2000, 3)), null,
                List.of(givenProducto(registro("Aceite Natura 900 ml", 2400, 8))));
    }

    /** La conversación de Don Pepe con una lista ya armada; {@code renglonACambiar} si espera elegir otra marca. */
    private void givenListaArmada(Integer renglonACambiar, LineaDeCompra... lineas) {
        givenComercios(DON_PEPE);
        givenClienteConocido(DON_PEPE.id());
        when(consumoSamiRepository.reservar(any(), any(), anyInt(), any())).thenReturn(true);
        when(conversacionRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(ConversacionCliente.builder()
                .clienteId(CLIENTE_ID).estado(EstadoConversacionCliente.PIDIENDO).negocioId(DON_PEPE.id())
                .lista(List.of(lineas)).renglonACambiar(renglonACambiar).actualizadoEn(AHORA.minusMinutes(5)).build()));
    }

    private void givenPidiendoADonPepe() {
        givenComercios(DON_PEPE);
        givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.PIDIENDO, DON_PEPE.id(), List.of());
        when(consumoSamiRepository.reservar(any(), any(), anyInt(), any())).thenReturn(true);
    }

    /** El inventario de Don Pepe: un proyecto con una tabla de inventario y estas filas. */
    private void givenStock(Registro... registros) {
        when(proyectoRepository.findAllByNegocioId(DON_PEPE.id())).thenReturn(List.of(proyectoConInventario()));
        when(registroRepository.findAllByBloqueIdAndNegocioId(INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(registros));
    }

    private static Registro registro(String nombre, Object precio, Object stock) {
        return PedidoTestData.registro(nombre, precio, stock, false);
    }

    private MensajeCliente givenMensajeReclamado(String texto) {
        MensajeCliente mensaje = givenMensaje(texto);
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of());
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    private static MensajeCliente givenMensaje(String texto) {
        return MensajeCliente.recibido(new MensajeEntrante(TELEFONO, "ID1", TipoMensajeWhatsapp.TEXTO, texto, null,
                null, AHORA));
    }

    private void givenComercios(Comercio... comercios) {
        when(comercioRepository.listarConWhatsapp()).thenReturn(List.of(comercios));
    }

    private static ClienteWhatsapp givenCliente(NegocioId ultimo) {
        return ClienteWhatsapp.builder().id(CLIENTE_ID).telefono(TELEFONO).ultimoNegocioId(ultimo).creadoEn(AHORA)
                .build();
    }

    private ClienteWhatsapp givenClienteConocido(NegocioId ultimo) {
        ClienteWhatsapp cliente = givenCliente(ultimo);
        when(clienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(cliente));
        return cliente;
    }

    private void givenSinConversacion() {
        when(conversacionRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.empty());
    }

    private void givenConversacionVigente(EstadoConversacionCliente estado, NegocioId negocioId,
                                          List<NegocioId> opciones) {
        givenConversacion(estado, negocioId, opciones, AHORA.minusMinutes(5));
    }

    private void givenConversacion(EstadoConversacionCliente estado, NegocioId negocioId, List<NegocioId> opciones,
                                   OffsetDateTime actualizadoEn) {
        when(conversacionRepository.findByClienteId(CLIENTE_ID)).thenReturn(Optional.of(ConversacionCliente.builder()
                .clienteId(CLIENTE_ID).estado(estado).negocioId(negocioId).opciones(opciones)
                .actualizadoEn(actualizadoEn).build()));
    }

    private ConversacionCliente thenConversacionGuardada() {
        ArgumentCaptor<ConversacionCliente> captor = ArgumentCaptor.forClass(ConversacionCliente.class);
        verify(conversacionRepository).save(captor.capture());
        return captor.getValue();
    }

    private void thenPidiendoA(Comercio comercio) {
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getNegocioId()).isEqualTo(comercio.id());
        assertThat(conversacion.getOpciones()).isEmpty();
    }

    private void thenMensajeGuardado(MensajeCliente mensaje, EstadoMensajeWhatsapp estado) {
        verify(mensajeRepository).save(mensaje);
        assertThat(mensaje.getEstado()).isEqualTo(estado);
    }
}
