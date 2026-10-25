package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion {

    private SuscripcionId id;

    private NegocioId negocioId;

    private PlanId planId;

    private EstadoSuscripcion estado;

    private OffsetDateTime inicioEn;

    private OffsetDateTime finEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public boolean estaVigente(OffsetDateTime ahora) {
        return (estado == EstadoSuscripcion.ACTIVA || estado == EstadoSuscripcion.PRUEBA) && !inicioEn.isAfter(ahora)
                && (finEn == null || finEn.isAfter(ahora));
    }
}
