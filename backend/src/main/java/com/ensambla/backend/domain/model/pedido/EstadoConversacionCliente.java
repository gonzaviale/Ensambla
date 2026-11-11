package com.ensambla.backend.domain.model.pedido;

/** En qué paso está un cliente con Sami: todavía elige a qué comercio pedirle, o ya le está pidiendo. */
public enum EstadoConversacionCliente {
    ELIGIENDO_COMERCIO,
    PIDIENDO
}
