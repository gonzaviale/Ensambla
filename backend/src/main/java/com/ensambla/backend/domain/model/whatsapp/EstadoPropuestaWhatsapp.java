package com.ensambla.backend.domain.model.whatsapp;

/** Etapa de una carga armada por Sami: espera que la persona elija la tabla o que la confirme, y después se cierra. */
public enum EstadoPropuestaWhatsapp {
    ELIGIENDO_TABLA,
    PENDIENTE,
    CONFIRMADA,
    DESCARTADA,
    VENCIDA
}
