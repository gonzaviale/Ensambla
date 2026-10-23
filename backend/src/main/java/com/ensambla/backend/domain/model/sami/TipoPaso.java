package com.ensambla.backend.domain.model.sami;

public enum TipoPaso {
    /** Sami necesita una aclaración: hay una pregunta para responder. */
    PREGUNTA,
    /** Sami ya sabe qué proponer. */
    PROPUESTA,
    /** El algoritmo propio no alcanzó la confianza ni quedan preguntas: corresponde consultar a la IA. */
    RESPALDO_IA
}
