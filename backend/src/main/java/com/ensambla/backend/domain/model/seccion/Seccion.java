package com.ensambla.backend.domain.model.seccion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Agrupa bloques dentro de un sistema ("Vista general", "Ventas"...). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seccion {

    private UUID id;

    private UUID negocioId;

    private UUID proyectoId;

    private String nombre;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
