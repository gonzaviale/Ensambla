package com.ensambla.backend.domain.model.seccion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Agrupa bloques dentro de un sistema ("Vista general", "Ventas"...). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seccion {

    private SeccionId id;

    private NegocioId negocioId;

    private ProyectoId proyectoId;

    private String nombre;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
