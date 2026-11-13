package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * El efecto de una decisión sobre las ventas, leído del aviso que se aplicó.
 *
 * @param deltaIngresoSemanal lo que cambió el ingreso semanal del producto, positivo si se vende más después; sólo
 *                            cuando el estado es {@link EstadoEfecto#MEDIDO}
 */
public record EfectoDecision(EstadoEfecto estado, BigDecimal deltaIngresoSemanal) {

    public static final EfectoDecision SIN_MEDICION = new EfectoDecision(EstadoEfecto.SIN_MEDICION, null);

    private static final EfectoDecision MIDIENDO = new EfectoDecision(EstadoEfecto.MIDIENDO, null);

    /**
     * El efecto de la decisión: sólo se mide el aviso aplicado. {@code aviso} es {@code null} si se borró o si la
     * decisión no es sobre un aviso.
     */
    public static EfectoDecision de(DecisionObjetivo decision, Aviso aviso) {
        if (decision.tipo() != TipoDecision.AVISO_APLICADO || aviso == null) {
            return SIN_MEDICION;
        }
        Optional<ImpactoAviso> impacto = aviso.impacto();
        if (impacto.isPresent()) {
            return new EfectoDecision(EstadoEfecto.MEDIDO, impacto.get().deltaIngresoSemanal());
        }
        return aviso.esMedible() ? MIDIENDO : SIN_MEDICION;
    }
}
