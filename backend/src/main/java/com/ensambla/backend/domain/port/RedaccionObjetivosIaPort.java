package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;

import java.util.List;

/**
 * IA que redacta el título y la justificación de los mini-objetivos. Sólo redacta: los números los calcula el
 * servidor y la IA no puede cambiarlos.
 */
public interface RedaccionObjetivosIaPort {

    /**
     * Puede lanzar cualquier excepción no verificada (también si la respuesta no sirve); quien llama usa entonces el
     * texto de reserva de cada candidato.
     */
    RedaccionObjetivosIa redactar(List<CandidatoMiniObjetivo> candidatos);
}
