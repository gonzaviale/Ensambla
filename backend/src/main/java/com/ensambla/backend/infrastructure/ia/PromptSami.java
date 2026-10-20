package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import org.springframework.core.io.Resource;

import java.util.stream.Collectors;

/**
 * Arma el pedido a la IA a partir de {@code ia/prompt-sami.md} y {@code ia/esquema-sami.json}. El contexto lista
 * las plantillas con sus columnas: es el vocabulario cerrado del que la IA no se puede salir.
 * Si cambia el prompt o el esquema, subir {@link #VERSION} y volver a medir el acierto.
 */
public class PromptSami {

    public static final String VERSION = "v1";

    private final String instrucciones;
    private final String esquemaJson;

    public PromptSami(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    public PeticionModeloTexto peticion(ConsultaIaSami consulta) {
        return new PeticionModeloTexto(instrucciones, contexto(consulta), esquemaJson);
    }

    private String contexto(ConsultaIaSami consulta) {
        StringBuilder contexto = new StringBuilder("Plantillas disponibles (elegí una por su nombre exacto):\n");
        consulta.plantillas().forEach(plantilla -> contexto.append("- %s: columnas %s\n"
                .formatted(plantilla.nombre(), columnas(plantilla))));
        contexto.append("\nLo que contó la persona:\n\"\"\"\n").append(consulta.texto()).append("\n\"\"\"\n");
        if (!consulta.aclaraciones().isEmpty()) {
            contexto.append("\nAclaraciones que ya dio:\n");
            consulta.aclaraciones().forEach(aclaracion -> contexto.append("- ").append(aclaracion).append('\n'));
        }
        return contexto.toString();
    }

    private String columnas(PlantillaProyecto plantilla) {
        return plantilla.estructura().bloques().stream()
                .flatMap(bloque -> bloque.campos().stream())
                .map(campo -> "%s (%s)".formatted(campo.nombre(), campo.tipo()))
                .collect(Collectors.joining(", "));
    }
}
