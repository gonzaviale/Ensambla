package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;

import java.util.List;
import java.util.Map;

/**
 * Un turno de Sami en el taller.
 *
 * @param acciones lo que el taller tiene que aplicar, ya validado
 * @param preguntas las dudas con respuestas rápidas del turno; vacía si no hay
 */
public record ConversarEnTallerResponse(
        String respuesta,
        List<AccionTallerItem> acciones,
        List<OpcionesTallerItem> preguntas
) {

    public record AccionTallerItem(String herramienta, Map<String, Object> argumentos) {
    }

    public record OpcionesTallerItem(String tema, String pregunta, List<String> opciones) {
    }

    public static ConversarEnTallerResponse de(RespuestaTaller respuesta) {
        List<AccionTallerItem> acciones = respuesta.acciones().stream()
                .map(accion -> new AccionTallerItem(accion.herramienta(), accion.argumentos()))
                .toList();
        List<OpcionesTallerItem> preguntas = respuesta.preguntas().stream()
                .map(duda -> new OpcionesTallerItem(duda.tema(), duda.pregunta(), duda.opciones()))
                .toList();
        return new ConversarEnTallerResponse(respuesta.texto(), acciones, preguntas);
    }
}
