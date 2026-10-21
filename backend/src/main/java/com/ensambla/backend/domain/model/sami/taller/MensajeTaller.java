package com.ensambla.backend.domain.model.sami.taller;

/** Un mensaje de la conversación reciente; {@code quien} es {@code sami} o {@code you}. */
public record MensajeTaller(String quien, String texto) {
}
