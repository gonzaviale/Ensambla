package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.NombresDePiezas;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.ValidacionTaller;
import com.ensambla.backend.domain.model.sami.taller.herramienta.RepertorioDeHerramientas;

import java.util.List;
import java.util.stream.Collectors;

/** Pone en palabras lo que Sami hizo en el turno. */
final class RedaccionDelTurno {

    private final RepertorioDeHerramientas herramientas;

    RedaccionDelTurno(RepertorioDeHerramientas herramientas) {
        this.herramientas = herramientas;
    }

    /**
     * Si el modelo no escribió nada (a veces sólo invoca herramientas), Sami cuenta lo que quedó hecho y, si hay
     * botonera, hace su pregunta. La charla muestra texto plano: se sacan las negritas de markdown que a veces escribe
     * el modelo ({@code **Turnos**}).
     */
    String texto(String delModelo, ValidacionTaller validacion, EstadoTaller estado) {
        if (delModelo != null && !delModelo.isBlank()) {
            return delModelo.replace("**", "").strip();
        }
        return textoDeLoHecho(validacion, estado);
    }

    /** Describe acciones sobre piezas que pueden venir por id, tipo o nombre (las sumadas en el turno no tienen id). */
    String describirTodas(List<AccionTaller> acciones, List<PiezaTaller> piezas) {
        return describir(acciones, NombresDePiezas.porCualquierReferencia(piezas));
    }

    private String textoDeLoHecho(ValidacionTaller validacion, EstadoTaller estado) {
        String pregunta = validacion.preguntas().isEmpty() ? null : validacion.preguntas().getFirst().pregunta();
        if (validacion.acciones().isEmpty()) {
            return pregunta == null ? ConversarEnTaller.SIN_CAMBIOS : pregunta;
        }
        String hechas = describir(validacion.acciones(), NombresDePiezas.porId(estado.piezas()));
        return hechas + ". Mirá cómo queda." + (pregunta == null ? "" : " " + pregunta);
    }

    private String describir(List<AccionTaller> acciones, NombresDePiezas nombres) {
        return acciones.stream()
                .map(accion -> herramientas.describir(accion, nombres))
                .collect(Collectors.joining(". "));
    }
}
