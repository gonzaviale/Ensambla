package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Por qué el cierre semanal cambia un mini-objetivo, y qué clase de cambio hace. */
public enum MotivoReajuste {
    /** Llegó a la meta. */
    LOGRADO(TipoReajuste.SUBIR_META,
            (anterior, valorAlCierre, nuevo) -> "Lo lograste: llegaste a "
                    + anterior.getTipo().decir(valorAlCierre) + ". Como ya no se puede pedir más, ahora probamos con «"
                    + nuevo.getTitulo() + "».") {
        @Override
        public boolean puedeSubirLaMeta() {
            return true;
        }

        @Override
        public void soltar(MiniObjetivo anterior, MiniObjetivoId nuevo, OffsetDateTime ahora) {
            anterior.lograrYSeguirCon(nuevo, ahora);
        }

        @Override
        public TipoReajuste tipoAlReemplazar() {
            return TipoReajuste.REEMPLAZAR;
        }
    },
    /** La persona lo quitó y el lugar quedó libre. */
    QUITADO(TipoReajuste.REEMPLAZAR,
            (anterior, valorAlCierre, nuevo) -> "Quitaste «" + anterior.getTitulo()
                    + "». En su lugar te proponemos «" + nuevo.getTitulo() + "»."),
    /** Su producto ya no está en el cuaderno o perdió el precio o el costo. */
    PRODUCTO_FUERA(TipoReajuste.REEMPLAZAR,
            (anterior, valorAlCierre, nuevo) -> "«" + anterior.getReferencia()
                    + "» ya no está en tu cuaderno o le falta el precio o el costo. Lo cambiamos por «"
                    + nuevo.getTitulo() + "»."),
    /** Se aplicó un aviso vinculado y, medido su efecto, las ventas no subieron. */
    DECISION_SIN_EFECTO(TipoReajuste.PROBAR_OTRO_CAMINO,
            (anterior, valorAlCierre, nuevo) -> "Aplicaste un aviso para «" + anterior.getTitulo()
                    + "» y las ventas no subieron. Probamos con «" + nuevo.getTitulo() + "»."),
    /** El valor al cierre no mejoró respecto del de dos cierres atrás. */
    SIN_AVANCE(TipoReajuste.REEMPLAZAR,
            (anterior, valorAlCierre, nuevo) -> "Hace 2 semanas que «" + anterior.getTitulo()
                    + "» no avanza. Lo cambiamos por «" + nuevo.getTitulo() + "».");

    /** Cómo se le cuenta a la persona por qué cambió el mini-objetivo. */
    @FunctionalInterface
    private interface Explicacion {

        String redactar(MiniObjetivo anterior, BigDecimal valorAlCierre, MiniObjetivo nuevo);
    }

    private final TipoReajuste tipo;

    private final Explicacion explicacion;

    MotivoReajuste(TipoReajuste tipo, Explicacion explicacion) {
        this.tipo = tipo;
        this.explicacion = explicacion;
    }

    public TipoReajuste tipo() {
        return tipo;
    }

    /** Si cuando se logra puede seguir el mismo mini-objetivo con una meta más alta. */
    public boolean puedeSubirLaMeta() {
        return false;
    }

    /** Cómo deja su lugar el mini-objetivo que se cambia por otro. */
    public void soltar(MiniObjetivo anterior, MiniObjetivoId nuevo, OffsetDateTime ahora) {
        anterior.cederLugarA(nuevo, ahora);
    }

    /** La clase de cambio que se anota cuando el mini-objetivo se cambia por otro candidato. */
    public TipoReajuste tipoAlReemplazar() {
        return tipo;
    }

    String explicar(MiniObjetivo anterior, BigDecimal valorAlCierre, MiniObjetivo nuevo) {
        return explicacion.redactar(anterior, valorAlCierre, nuevo);
    }
}
