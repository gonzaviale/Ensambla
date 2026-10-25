package com.ensambla.backend.domain.model.plantilla;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

/** Plantilla global (no pertenece a ningún negocio): estructura de bloques y campos copiable. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plantilla {

    private PlantillaId id;

    private String nombre;

    private String descripcion;

    private String icono;

    /** Bloques y campos de la plantilla, tal como se copian al crear un proyecto. */
    private Map<String, Object> estructura;

    private Boolean activa;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
