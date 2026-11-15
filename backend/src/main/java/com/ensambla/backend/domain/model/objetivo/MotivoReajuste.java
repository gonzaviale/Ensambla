package com.ensambla.backend.domain.model.objetivo;

/** Por qué el cierre semanal cambia un mini-objetivo, y qué clase de cambio hace. */
public enum MotivoReajuste {
    /** Llegó a la meta. */
    LOGRADO(TipoReajuste.SUBIR_META),
    /** La persona lo quitó y el lugar quedó libre. */
    QUITADO(TipoReajuste.REEMPLAZAR),
    /** Su producto ya no está en el cuaderno o perdió el precio o el costo. */
    PRODUCTO_FUERA(TipoReajuste.REEMPLAZAR),
    /** Se aplicó un aviso vinculado y, medido su efecto, las ventas no subieron. */
    DECISION_SIN_EFECTO(TipoReajuste.PROBAR_OTRO_CAMINO),
    /** El valor al cierre no mejoró respecto del de dos cierres atrás. */
    SIN_AVANCE(TipoReajuste.REEMPLAZAR);

    private final TipoReajuste tipo;

    MotivoReajuste(TipoReajuste tipo) {
        this.tipo = tipo;
    }

    public TipoReajuste tipo() {
        return tipo;
    }
}
