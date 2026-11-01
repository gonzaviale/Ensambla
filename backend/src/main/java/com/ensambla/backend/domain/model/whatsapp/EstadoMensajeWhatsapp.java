package com.ensambla.backend.domain.model.whatsapp;

/** Etapa de un mensaje en la cola de atención: se guarda, se reclama, y termina atendido o fallido. */
public enum EstadoMensajeWhatsapp {
    RECIBIDO,
    ATENDIENDO,
    ATENDIDO,
    FALLIDO
}
