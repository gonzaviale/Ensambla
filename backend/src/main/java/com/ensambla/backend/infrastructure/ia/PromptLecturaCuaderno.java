package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Arma la petición al modelo a partir de {@code ia/prompt-lectura-cuaderno.md} y {@code ia/esquema-lectura-cuaderno.json}.
 * Si cambia el prompt o el esquema, subir {@link #VERSION} y correr la medición de precisión (T-099).
 */
public class PromptLecturaCuaderno {

    public static final String VERSION = "v1";

    static final String SIN_BLOQUES = "El negocio todavía no tiene bloques: proponé uno nuevo.";
    static final String ENCABEZADO_BLOQUES = "Bloques que el negocio ya tiene (id: nombre [campos]):";

    private final String instrucciones;
    private final String esquemaJson;

    public PromptLecturaCuaderno(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    public PeticionModeloVision peticion(List<PaginaCuaderno> paginas, List<BloqueConocido> bloquesExistentes) {
        return new PeticionModeloVision(instrucciones, contexto(bloquesExistentes), List.copyOf(paginas), esquemaJson);
    }

    private String contexto(List<BloqueConocido> bloquesExistentes) {
        if (bloquesExistentes.isEmpty()) {
            return SIN_BLOQUES;
        }
        return bloquesExistentes.stream()
                .map(bloque -> "- %s: %s [%s]".formatted(bloque.id(), bloque.nombre(), String.join(", ", bloque.campos())))
                .collect(Collectors.joining("\n", ENCABEZADO_BLOQUES + "\n", ""));
    }
}
