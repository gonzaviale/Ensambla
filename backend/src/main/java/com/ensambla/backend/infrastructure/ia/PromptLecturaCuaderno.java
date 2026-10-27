package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Arma la petición al modelo a partir de {@code ia/prompt-lectura-cuaderno.md} y {@code ia/esquema-lectura-cuaderno.json}.
 * Si cambia el prompt o el esquema, subir {@link #VERSION} y correr la medición de precisión (T-099).
 */
public class PromptLecturaCuaderno {

    public static final String VERSION = "v3";

    static final String SIN_BLOQUES = "El negocio todavía no tiene bloques: proponé uno nuevo.";
    static final String ENCABEZADO_BLOQUES = "Bloques que el negocio ya tiene (id: nombre [campo (TIPO), ...]):";
    static final String ENCABEZADO_DESTINO = "DESTINO FIJO: la persona indicó que estas fotos son filas del bloque"
            + " existente que sigue (id: nombre [campo (TIPO), ...]). Devolvé su id en bloqueExistenteId y usá"
            + " exactamente el mismo nombre de bloque. Las columnas de la foto que el bloque ya tiene van con el mismo"
            + " nombre, tipo y opciones. Si la foto trae una columna que el bloque no tiene, agregala como campo nuevo"
            + " con un nombre legible y el tipo que sugieran sus valores (nunca RELACION). Incluí sólo las columnas"
            + " que están escritas en la foto. Si las fotos no tienen nada que ver con este bloque, devolvé filas"
            + " vacías y explicalo en \"advertencias\".";

    private final String instrucciones;
    private final String esquemaJson;

    public PromptLecturaCuaderno(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    /**
     * @param bloqueDestinoId bloque elegido por la persona antes de subir las fotos; con destino la IA sólo ve ese
     *                        bloque y no tiene que adivinar a cuál sumar las filas
     */
    public PeticionModeloVision peticion(List<PaginaCuaderno> paginas, List<BloqueConocido> bloquesExistentes,
                                         BloqueId bloqueDestinoId) {
        return new PeticionModeloVision(
                instrucciones, contexto(bloquesExistentes, bloqueDestinoId), List.copyOf(paginas), esquemaJson);
    }

    private String contexto(List<BloqueConocido> bloquesExistentes, BloqueId bloqueDestinoId) {
        if (bloqueDestinoId != null) {
            return lista(ENCABEZADO_DESTINO, bloquesExistentes.stream()
                    .filter(bloque -> bloque.id().equals(bloqueDestinoId))
                    .toList());
        }
        if (bloquesExistentes.isEmpty()) {
            return SIN_BLOQUES;
        }
        return lista(ENCABEZADO_BLOQUES, bloquesExistentes);
    }

    private String lista(String encabezado, List<BloqueConocido> bloques) {
        return bloques.stream()
                .map(bloque -> "- %s: %s [%s]".formatted(bloque.id(), bloque.nombre(), campos(bloque)))
                .collect(Collectors.joining("\n", encabezado + "\n", ""));
    }

    private String campos(BloqueConocido bloque) {
        return bloque.campos().stream().map(this::campo).collect(Collectors.joining(", "));
    }

    private String campo(CampoContexto campo) {
        if (campo.bloqueRelacionado() != null) {
            return "%s (%s → %s: devolvé el texto tal como está escrito)"
                    .formatted(campo.nombre(), campo.tipo(), campo.bloqueRelacionado());
        }
        if (campo.opciones().isEmpty()) {
            return "%s (%s)".formatted(campo.nombre(), campo.tipo());
        }
        return "%s (%s: %s)".formatted(campo.nombre(), campo.tipo(), String.join("|", campo.opciones()));
    }
}
