package com.ensambla.backend.domain.useCase.pedido.atenderMensajeCliente;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.MensajeClienteRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Atiende, de a uno y en orden, lo que escribieron los clientes por WhatsApp. Registra al cliente la primera vez que
 * escribe y le contesta; la conversación del pedido se arma sobre este caso de uso.
 */
@RequiredArgsConstructor
public class AtenderMensajeCliente {

    private static final String MOTIVO_PERDIDO = "Se perdió mientras se atendía";

    private final MensajeClienteRepository mensajeRepository;
    private final ClienteWhatsappRepository clienteRepository;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;
    private final Duration vigenciaReclamo;

    /** @return si había un mensaje esperando; quien lo llama insiste mientras haya */
    public boolean execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        recuperarPerdidos(ahora);
        Optional<MensajeCliente> reclamado = mensajeRepository.reclamarSiguiente(ahora, vigenciaReclamo);
        reclamado.ifPresent(this::atender);
        return reclamado.isPresent();
    }

    /** Un mensaje reclamado que nunca terminó (se cayó el proceso): no se reintenta, para no contestar dos veces. */
    private void recuperarPerdidos(OffsetDateTime ahora) {
        for (MensajeCliente perdido : mensajeRepository.vencidos(ahora)) {
            perdido.fallar(MOTIVO_PERDIDO, ahora);
            mensajeRepository.save(perdido);
            mensajeria.enviarTexto(perdido.getTelefono(), TextosWhatsapp.ERROR_TEMPORAL);
        }
    }

    private void atender(MensajeCliente mensaje) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        try {
            transaccionPort.ejecutar(() -> registrarCliente(mensaje.getTelefono(), ahora));
            mensajeria.enviarTexto(mensaje.getTelefono(), TextosWhatsapp.BIENVENIDA_CLIENTE);
            mensaje.atender(ahora);
        } catch (BusinessRuleException e) {
            mensajeria.enviarTexto(mensaje.getTelefono(), e.getMessage());
            mensaje.atender(ahora);
        } catch (RuntimeException e) {
            mensajeria.enviarTexto(mensaje.getTelefono(), TextosWhatsapp.ERROR_TEMPORAL);
            mensaje.fallar(e.getClass().getSimpleName() + ": " + e.getMessage(), ahora);
        }
        mensajeRepository.save(mensaje);
    }

    private ClienteWhatsapp registrarCliente(String telefono, OffsetDateTime ahora) {
        return clienteRepository.findByTelefono(telefono)
                .orElseGet(() -> clienteRepository.save(ClienteWhatsapp.nuevo(telefono, ahora)));
    }
}
