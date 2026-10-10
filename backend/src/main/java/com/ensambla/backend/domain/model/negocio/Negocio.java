package com.ensambla.backend.domain.model.negocio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Espacio del negocio: todo lo que construye el usuario cuelga de él (aislamiento de datos). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

    private UUID id;

    private String nombre;

    private UUID duenioId;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
