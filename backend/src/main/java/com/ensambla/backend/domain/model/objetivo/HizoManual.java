package com.ensambla.backend.domain.model.objetivo;

import java.time.OffsetDateTime;
import java.util.function.Function;

/** Si la persona hizo la acción de un mini-objetivo manual esa semana. */
public enum HizoManual {
    SI(TipoDecision.MINI_HECHO,
            quePaso -> quePaso.cambiaLaAccion() ? AccionRenovacion.CAMBIA : AccionRenovacion.SUBE_UN_PASO) {
        @Override
        public void cerrarAnterior(MiniObjetivo anterior, MiniObjetivoId nuevo, OffsetDateTime ahora) {
            anterior.lograrYSeguirCon(nuevo, ahora);
        }
    },
    EN_PARTE(TipoDecision.MINI_EN_PARTE,
            quePaso -> AccionRenovacion.SE_DIVIDE),
    NO(TipoDecision.MINI_NO_HECHO,
            quePaso -> quePaso.cambiaLaAccion() ? AccionRenovacion.CAMBIA : AccionRenovacion.SE_SIMPLIFICA);

    private final TipoDecision tipoDecision;

    private final Function<QuePaso, AccionRenovacion> renovacion;

    HizoManual(TipoDecision tipoDecision, Function<QuePaso, AccionRenovacion> renovacion) {
        this.tipoDecision = tipoDecision;
        this.renovacion = renovacion;
    }

    /** La decisión que queda anotada en el historial del objetivo. */
    public TipoDecision tipoDecision() {
        return tipoDecision;
    }

    /** Cómo deja su lugar el mini-objetivo respondido cuando el domingo lo renueva otro. */
    public void cerrarAnterior(MiniObjetivo anterior, MiniObjetivoId nuevo, OffsetDateTime ahora) {
        anterior.reemplazarPor(nuevo, ahora);
    }

    /** Cómo se renueva el mini-objetivo el domingo según lo que pasó. */
    AccionRenovacion renovacionSegun(QuePaso quePaso) {
        return renovacion.apply(quePaso);
    }
}
