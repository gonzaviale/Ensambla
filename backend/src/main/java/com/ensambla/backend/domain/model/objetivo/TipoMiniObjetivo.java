package com.ensambla.backend.domain.model.objetivo;

/** Qué clase de cambio o de meta es un mini-objetivo y qué métrica mide. */
public enum TipoMiniObjetivo {
    /** Ganancia semanal de un producto con margen bajo. */
    SUBIR_PRECIO(SentidoMiniObjetivo.SUBE, true),
    /** Unidades por semana de un producto parado. */
    MOVER_PARADO(SentidoMiniObjetivo.SUBE, true),
    /** Unidades por semana del producto que más deja por unidad. */
    PONER_A_LA_VISTA(SentidoMiniObjetivo.SUBE, true),
    /** Gasto de 30 días de una categoría. */
    RECORTAR_GASTO(SentidoMiniObjetivo.BAJA, false),
    /** Margen bruto de 30 días, en puntos. */
    MEJORAR_MARGEN(SentidoMiniObjetivo.SUBE, false),
    /** Ventas de los últimos 7 días. */
    VENTAS_SEMANALES(SentidoMiniObjetivo.SUBE, false);

    private final SentidoMiniObjetivo sentido;

    private final boolean deProducto;

    TipoMiniObjetivo(SentidoMiniObjetivo sentido, boolean deProducto) {
        this.sentido = sentido;
        this.deProducto = deProducto;
    }

    public SentidoMiniObjetivo sentido() {
        return sentido;
    }

    /** Si apunta a un producto del cuaderno (y por eso guarda su {@code registroId}). */
    public boolean esDeProducto() {
        return deProducto;
    }
}
