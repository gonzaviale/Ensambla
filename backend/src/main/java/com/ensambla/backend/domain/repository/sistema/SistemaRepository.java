package com.ensambla.backend.domain.repository.sistema;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SistemaRepository {

    /** Todos los sistemas del negocio, con secciones, columnas y filas. Nunca incluye los de otro negocio. */
    List<Sistema> findAllByNegocioId(NegocioId negocioId);

    /** Negocio dueño del proyecto, para distinguir "no existe" de "es de otro" antes de escribir. */
    Optional<NegocioId> findNegocioIdByProyectoId(ProyectoId proyectoId);

    /**
     * Crea o reemplaza el sistema entero en una transacción: lo que ya existe conserva su id, lo nuevo se inserta y
     * lo que ya no está se elimina, salvo las filas que cargó Sami por WhatsApp después de {@code leidoEn}: quien guarda no las conocía,
     * así que no las borró.
     *
     * @param leidoEn cuándo leyó el cliente el sistema que está reemplazando; null elimina todo lo que no venga
     */
    void guardar(Sistema sistema, OffsetDateTime leidoEn);

    /** Reemplazo exacto: elimina todo lo que no venga en el sistema. */
    default void guardar(Sistema sistema) {
        guardar(sistema, null);
    }
}
