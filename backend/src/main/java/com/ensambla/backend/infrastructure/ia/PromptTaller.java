package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import org.springframework.core.io.Resource;

import java.util.stream.Collectors;

/**
 * Arma el pedido a la IA para un turno del taller: las instrucciones fijas ({@code ia/prompt-taller.md}) y un contexto
 * corto con el sistema actual, la charla reciente y el mensaje. Sin filas ni datos de ejemplo, para que el turno
 * cueste pocos tokens. Si cambia el prompt o las herramientas, subir {@link #VERSION} y volver a medir el acierto.
 */
public class PromptTaller {

    public static final String VERSION = "v1";
    static final String MARCA_MENSAJE = "Mensaje de la persona:";

    private final String instrucciones;

    public PromptTaller(Resource instrucciones) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
    }

    public PeticionConHerramientas peticion(ConsultaTaller consulta) {
        return new PeticionConHerramientas(instrucciones, contexto(consulta));
    }

    private String contexto(ConsultaTaller consulta) {
        StringBuilder contexto = new StringBuilder();
        contexto.append("Sistema actual: «%s» (distribución: %s)\n"
                .formatted(consulta.estado().nombre(), consulta.estado().distribucion()));
        if (consulta.estado().piezas().isEmpty()) {
            contexto.append("Todavía no tiene piezas.\n");
        } else {
            consulta.estado().piezas().forEach(pieza -> contexto.append(pieza(pieza)));
        }
        if (!consulta.historial().isEmpty()) {
            contexto.append("\nConversación reciente:\n");
            consulta.historial().forEach(mensaje -> contexto.append("%s: %s\n"
                    .formatted(mensaje.quien().equals("sami") ? "Sami" : "Persona", mensaje.texto())));
        }
        return contexto.append('\n').append(MARCA_MENSAJE).append(' ').append(consulta.mensaje()).toString();
    }

    private String pieza(PiezaTaller pieza) {
        String campos = pieza.campos().stream()
                .map((CampoTaller campo) -> "%s (%s)".formatted(campo.etiqueta(), campo.tipo()))
                .collect(Collectors.joining(", "));
        String seccion = pieza.seccion() == null ? "" : " sección «%s»".formatted(pieza.seccion());
        return "- id %s, tipo %s, «%s»%s: %s\n".formatted(pieza.id(), pieza.tipo(), pieza.nombre(), seccion, campos);
    }
}
