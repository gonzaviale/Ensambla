package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.campo.Campo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bloque {

    private UUID id;

    private UUID negocioId;

    private UUID proyectoId;

    private String nombre;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    @Builder.Default
    private List<Campo> campos = List.of();
}
