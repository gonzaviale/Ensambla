package com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.Comercio;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.EstadoConversacionCliente;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.TextosPedido;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.ComercioRepository;
import com.ensambla.backend.domain.repository.pedido.ConversacionClienteRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private static final NegocioId CERRADO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000009");

    private final MensajeClienteRepository mensajeRepository = mock(MensajeClienteRepository.class);
    private final ClienteWhatsappRepository clienteRepository = mock(ClienteWhatsappRepository.class);
    private final ConversacionClienteRepository conversacionRepository = mock(ConversacionClienteRepository.class);
    private final ComercioRepository comercioRepository = mock(ComercioRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeCliente atenderMensajeCliente = new AtenderMensajeCliente(mensajeRepository,
            clienteRepository, conversacionRepository, comercioRepository, mensajeria, new TransaccionDirecta(),
            Clock.fixed(INSTANTE, ZoneOffset.UTC), VIGENCIA);

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
    @DisplayName("Ya con el comercio elegido, el pedido todavía no se arma y se lo avisa sin salir de la conversación")
    void executeShouldKeepTheBusinessWhileOrdering() {
        givenMensajeReclamado("dame 1 kilo de yerba");
        givenComercios(DON_PEPE);
        givenClienteConocido(DON_PEPE.id());
        givenConversacionVigente(EstadoConversacionCliente.PIDIENDO, DON_PEPE.id(), List.of());

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosPedido.PEDIDO_EN_CONSTRUCCION);
        ConversacionCliente conversacion = thenConversacionGuardada();
        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacionCliente.PIDIENDO);
        assertThat(conversacion.getActualizadoEn()).isEqualTo(AHORA);
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

    // --- datos de prueba ---

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
