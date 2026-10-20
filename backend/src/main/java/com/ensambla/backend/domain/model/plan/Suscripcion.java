package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
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
public class Suscripcion {

    private UUID id;

    private UUID negocioId;

    private UUID planId;

    private EstadoSuscripcion estado;

    private OffsetDateTime inicioEn;

    private OffsetDateTime finEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public boolean estaVigente(OffsetDateTime ahora) {
        return estado == EstadoSuscripcion.ACTIVA && !inicioEn.isAfter(ahora)
                && (finEn == null || finEn.isAfter(ahora));
    }
}
