package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Optional;

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
        return (estaActiva() || estado == EstadoSuscripcion.PRUEBA) && !inicioEn.isAfter(ahora)
                && (finEn == null || finEn.isAfter(ahora));
    }

    public boolean estaActiva() {
        return estado == EstadoSuscripcion.ACTIVA;
    }

    public boolean esDelPlan(PlanId otroPlanId) {
        return planId.equals(otroPlanId);
    }

    public boolean tienePlanVigente(PlanId otroPlanId, OffsetDateTime ahora) {
        return estaVigente(ahora) && esDelPlan(otroPlanId);
    }

    public boolean estaEnPrueba() {
        return estado == EstadoSuscripcion.PRUEBA;
    }

    /** Una prueba cuyo plazo ya pasó: es la que se vuelve al plan gratuito. */
    public boolean tieneLaPruebaVencida(OffsetDateTime ahora) {
        return estaEnPrueba() && !finEn.isAfter(ahora);
    }

    public void pasarAPrueba(PlanId premiumId, PruebaPremium prueba, OffsetDateTime ahora) {
        this.planId = premiumId;
        this.estado = EstadoSuscripcion.PRUEBA;
        this.inicioEn = prueba.inicioEn();
        this.finEn = prueba.finEn();
        this.actualizadoEn = ahora;
    }

    public void volverAlPlanGratuito(PlanId gratuitoId, OffsetDateTime ahora) {
        activarPlan(gratuitoId, ahora, null, ahora);
    }

    public void activarPlan(PlanId nuevoPlanId, OffsetDateTime desde, OffsetDateTime hasta, OffsetDateTime ahora) {
        this.planId = nuevoPlanId;
        this.estado = EstadoSuscripcion.ACTIVA;
        this.inicioEn = desde;
        this.finEn = hasta;
        this.actualizadoEn = ahora;
    }

    /** Hasta cuándo dura el plan premium ya vigente; vacío si todavía no lo tiene. */
    public Optional<OffsetDateTime> vencimientoDePremium(PlanId premiumId, OffsetDateTime ahora) {
        if (!estaVigente(ahora) || !esDelPlan(premiumId)) {
            return Optional.empty();
        }
        return Optional.of(Optional.ofNullable(finEn).orElseThrow(
                () -> new BusinessRuleException("Tu negocio ya tiene Premium sin vencimiento.")));
    }
}
