package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Una duda de Sami con respuestas rápidas. Elegir una opción equivale a escribir su texto; la pantalla siempre suma
 * además la posibilidad de escribir otra cosa o de saltearla. Sami puede plantear varias en un mismo turno: la pantalla
 * las muestra de a una y manda todas las respuestas juntas.
 *
 * @param tema palabra o frase corta que nombra la duda («Tildes»); la pantalla arma con ella la respuesta. Puede ser
 *             {@code null}: entonces se usa la pregunta
 */
public record OpcionesTaller(String tema, String pregunta, List<String> opciones) {

    public OpcionesTaller(String pregunta, List<String> opciones) {
        this(null, pregunta, opciones);
    }
}
