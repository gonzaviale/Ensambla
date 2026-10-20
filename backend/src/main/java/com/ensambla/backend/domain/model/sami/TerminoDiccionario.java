package com.ensambla.backend.domain.model.sami;

/**
 * Palabra o frase que suma {@code peso} puntos a una plantilla cuando aparece en lo que escribió la persona.
 * El texto se normaliza igual que el de la persona, así que alcanza con escribirlo de forma natural.
 */
public record TerminoDiccionario(String termino, int peso) {
}
