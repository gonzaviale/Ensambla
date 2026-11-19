package com.ensambla.backend.domain.model.plan;

/**
 * Los objetivos con IA que el negocio ya guardó este mes y los que su plan permite.
 *
 * @param limite vacío si el plan no tiene límite
 */
public record CuotaObjetivosIa(long usadas, Integer limite) {

    /** Cuántos objetivos con IA más puede guardar este mes; vacío si no hay límite. */
    public Integer restantes() {
        return limite == null ? null : (int) Math.max(0, limite - usadas);
    }

    public boolean agotada() {
        return limite != null && usadas >= limite;
    }
}
