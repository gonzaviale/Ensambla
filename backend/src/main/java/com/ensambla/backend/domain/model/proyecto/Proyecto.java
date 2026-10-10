package com.ensambla.backend.domain.model.proyecto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proyecto {

    private UUID id;

    private UUID negocioId;

    private String nombre;

    private String descripcion;

    /** Plantilla de origen; null si se creó en blanco o si la plantilla ya no existe. */
    private UUID plantillaId;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
