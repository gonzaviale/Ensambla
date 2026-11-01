package com.ensambla.backend.domain.useCase.aviso.contarAvisos;

/** Avisos pendientes del negocio; {@code urgentes} son los de prioridad 2 o 3, que van resaltados en la campana. */
public record ContadorAvisos(long pendientes, long urgentes) {
}
