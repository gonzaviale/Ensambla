package com.ensambla.backend.infrastructure.ia;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Lee recursos de texto de {@code classpath:ia/} al arrancar; si falta alguno la aplicación no levanta. */
final class RecursoTexto {

    private RecursoTexto() {
    }

    static String contenido(Resource recurso) {
        try {
            return recurso.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el recurso de IA " + recurso.getDescription(), e);
        }
    }
}
