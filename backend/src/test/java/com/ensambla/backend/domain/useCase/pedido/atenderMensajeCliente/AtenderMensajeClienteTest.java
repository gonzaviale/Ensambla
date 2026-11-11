package com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
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

    private final MensajeClienteRepository mensajeRepository = mock(MensajeClienteRepository.class);
    private final ClienteWhatsappRepository clienteRepository = mock(ClienteWhatsappRepository.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeCliente atenderMensajeCliente = new AtenderMensajeCliente(mensajeRepository,
            clienteRepository, mensajeria, new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC), VIGENCIA);

    @Test
    @DisplayName("Sin mensajes en la cola no hace nada y avisa que no había")
    void executeShouldReturnFalseWhenQueueIsEmpty() {
        givenColaVacia();

        boolean habia = atenderMensajeCliente.execute();

        assertThat(habia).isFalse();
        verify(mensajeria, never()).enviarTexto(anyString(), anyString());
    }

    @Test
    @DisplayName("El primer mensaje de un cliente lo registra, le contesta y cierra el mensaje como atendido")
    void executeShouldRegisterNewClientAndReply() {
        MensajeCliente mensaje = givenMensajeReclamado();
        when(clienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());

        boolean habia = atenderMensajeCliente.execute();

        assertThat(habia).isTrue();
        ArgumentCaptor<ClienteWhatsapp> captor = ArgumentCaptor.forClass(ClienteWhatsapp.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getTelefono()).isEqualTo(TELEFONO);
        assertThat(captor.getValue().getCreadoEn()).isEqualTo(AHORA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.BIENVENIDA_CLIENTE);
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.ATENDIDO);
    }

    @Test
    @DisplayName("Un cliente que ya escribió antes no se registra otra vez")
    void executeShouldNotRegisterKnownClientAgain() {
        givenMensajeReclamado();
        when(clienteRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(ClienteWhatsapp.nuevo(TELEFONO, AHORA)));

        atenderMensajeCliente.execute();

        verify(clienteRepository, never()).save(any());
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.BIENVENIDA_CLIENTE);
    }

    @Test
    @DisplayName("Una regla de negocio rota se le dice al cliente y el mensaje queda atendido")
    void executeShouldReplyBusinessRuleMessage() {
        MensajeCliente mensaje = givenMensajeReclamado();
        doThrow(new BusinessRuleException("No se puede")).when(clienteRepository).findByTelefono(TELEFONO);

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, "No se puede");
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.ATENDIDO);
    }

    @Test
    @DisplayName("Un error inesperado le pide al cliente que pruebe de nuevo y deja el mensaje fallido con el motivo")
    void executeShouldFailTheMessageOnUnexpectedError() {
        MensajeCliente mensaje = givenMensajeReclamado();
        doThrow(new IllegalStateException("Base no disponible")).when(clienteRepository).findByTelefono(TELEFONO);

        atenderMensajeCliente.execute();

        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
        thenMensajeGuardado(mensaje, EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).isEqualTo("IllegalStateException: Base no disponible");
    }

    @Test
    @DisplayName("Un mensaje reclamado que nunca terminó se da por perdido, se avisa al cliente y no se reintenta")
    void executeShouldFailLostMessages() {
        MensajeCliente perdido = givenMensaje();
        perdido.reclamar(AHORA.minusMinutes(10), VIGENCIA);
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of(perdido));
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.empty());

        atenderMensajeCliente.execute();

        assertThat(perdido.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(perdido.getError()).isEqualTo("Se perdió mientras se atendía");
        verify(mensajeRepository).save(perdido);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
    }

    private void givenColaVacia() {
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of());
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.empty());
    }

    private MensajeCliente givenMensajeReclamado() {
        MensajeCliente mensaje = givenMensaje();
        when(mensajeRepository.vencidos(AHORA)).thenReturn(List.of());
        when(mensajeRepository.reclamarSiguiente(AHORA, VIGENCIA)).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    private static MensajeCliente givenMensaje() {
        return MensajeCliente.recibido(new MensajeEntrante(TELEFONO, "ID1", TipoMensajeWhatsapp.TEXTO, "hola", null,
                null, AHORA));
    }

    private void thenMensajeGuardado(MensajeCliente mensaje, EstadoMensajeWhatsapp estado) {
        verify(mensajeRepository).save(mensaje);
        assertThat(mensaje.getEstado()).isEqualTo(estado);
    }
}
