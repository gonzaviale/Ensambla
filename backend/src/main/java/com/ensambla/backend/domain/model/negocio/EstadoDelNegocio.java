package com.ensambla.backend.domain.model.negocio;

/** Cómo viene un negocio en la vista conjunta de la cuenta, comparable entre rubros distintos. */
public enum EstadoDelNegocio {

    /** Vendió en el período y le queda plata después de la mercadería y los gastos. */
    BIEN,

    /** Vendió en el período, pero lo que gastó supera lo que vendió. */
    ATENCION,

    /** No anotó ventas en el período: todavía no hay con qué medirlo. */
    SIN_DATOS;

    public static EstadoDelNegocio de(boolean hayVentas, long margen) {
        if (!hayVentas) {
            return SIN_DATOS;
        }
        return margen < 0 ? ATENCION : BIEN;
    }
}
