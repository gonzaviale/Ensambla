package com.ensambla.backend.domain.model.metrica;

/** Qué se calcula con las filas de la tabla. Siempre lo hace el servidor, nunca la IA. */
public enum CalculoMetrica {

    /** Suma del importe: ventas = precio × cantidad, gastos = total, inventario = cantidad × costo. */
    SUMA(FormatoMetrica.MONEDA),
    /** Importe promedio por fila. */
    PROMEDIO(FormatoMetrica.MONEDA),
    /** Cuántas filas hay. */
    CONTEO(FormatoMetrica.NUMERO),
    /** Suma de las unidades (cantidad); los gastos no tienen. */
    UNIDADES(FormatoMetrica.NUMERO);

    private final FormatoMetrica formato;

    CalculoMetrica(FormatoMetrica formato) {
        this.formato = formato;
    }

    public FormatoMetrica formato() {
        return formato;
    }

    /** {@code true} si necesita el importe de cada fila (precio, total o costo). */
    public boolean usaImporte() {
        return formato == FormatoMetrica.MONEDA;
    }

    public boolean sirvePara(FuenteMetrica fuente) {
        return this != UNIDADES || fuente != FuenteMetrica.GASTOS;
    }
}
