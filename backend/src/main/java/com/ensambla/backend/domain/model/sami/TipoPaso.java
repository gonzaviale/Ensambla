package com.ensambla.backend.domain.model.sami;

public enum TipoPaso {
    /** Sami necesita una aclaración: hay una pregunta para responder. */
    PREGUNTA(false),
    /** Sami ya sabe qué proponer. */
    PROPUESTA(true),
    /** El algoritmo propio no alcanzó la confianza ni quedan preguntas: corresponde consultar a la IA. */
    RESPALDO_IA(false);

    private final boolean traePropuesta;

    TipoPaso(boolean traePropuesta) {
        this.traePropuesta = traePropuesta;
    }

    /** Si en este paso Sami ya tiene una propuesta para mostrar. */
    public boolean traePropuesta() {
        return traePropuesta;
    }
}