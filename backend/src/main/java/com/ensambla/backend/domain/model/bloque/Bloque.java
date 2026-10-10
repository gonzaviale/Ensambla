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

    public static Bloque nuevo(UUID negocioId, UUID proyectoId, String nombre, short posicion, OffsetDateTime ahora) {
        return Bloque.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre.strip())
                .posicion(posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public void renombrar(String nombre, OffsetDateTime ahora) {
        this.nombre = nombre.strip();
        this.actualizadoEn = ahora;
    }
}
