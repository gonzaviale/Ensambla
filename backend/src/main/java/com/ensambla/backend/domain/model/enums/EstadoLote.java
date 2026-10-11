package com.ensambla.backend.domain.model.enums;

/** Ciclo de vida de un lote de fotos: se sube, la IA lo lee y el usuario lo confirma o descarta. */
public enum EstadoLote {
    PENDIENTE,
    PROCESANDO,
    LISTO,
    ERROR,
    CONFIRMADO,
    DESCARTADO
}
