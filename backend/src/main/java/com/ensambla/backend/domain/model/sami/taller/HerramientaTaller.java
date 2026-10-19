package com.ensambla.backend.domain.model.sami.taller;

import java.util.Arrays;
import java.util.Optional;

/** Las herramientas que Sami puede invocar en el taller. {@code nombre} es el que ve el modelo y el frontend. */
public enum HerramientaTaller {
    AGREGAR_PIEZA("agregar_pieza"),
    QUITAR_PIEZA("quitar_pieza"),
    RENOMBRAR_PIEZA("renombrar_pieza"),
    RENOMBRAR_SISTEMA("renombrar_sistema"),
    AGREGAR_CAMPO("agregar_campo"),
    QUITAR_CAMPO("quitar_campo"),
    SEPARAR_EN_SECCION("separar_en_seccion"),
    CAMBIAR_DISTRIBUCION("cambiar_distribucion"),
    /** No cambia el sistema: es una pregunta con respuestas rápidas. */
    OFRECER_OPCIONES("ofrecer_opciones");

    private final String nombre;

    HerramientaTaller(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }

    public static Optional<HerramientaTaller> deNombre(String nombre) {
        return Arrays.stream(values()).filter(herramienta -> herramienta.nombre.equals(nombre)).findFirst();
    }
}
