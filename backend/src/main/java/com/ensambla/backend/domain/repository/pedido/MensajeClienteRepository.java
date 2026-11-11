package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.model.pedido.MensajeCliente;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MensajeClienteRepository {

    MensajeCliente save(MensajeCliente mensaje);

    boolean existsByIdExterno(String idExterno);

    /**
     * Toma el mensaje recibido más viejo para atenderlo: lo deja {@code ATENDIENDO} por {@code vigencia}. Dos
     * atenciones simultáneas nunca reciben el mismo.
     */
    Optional<MensajeCliente> reclamarSiguiente(OffsetDateTime ahora, Duration vigencia);

    /** Los mensajes reclamados cuyo plazo ya pasó: quien los tomó no los terminó. */
    List<MensajeCliente> vencidos(OffsetDateTime ahora);
}
