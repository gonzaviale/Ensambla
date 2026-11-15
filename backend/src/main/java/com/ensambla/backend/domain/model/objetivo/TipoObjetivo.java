package com.ensambla.backend.domain.model.objetivo;

import java.util.Collection;

/** Qué clase de objetivo es, según tenga o no una meta en pesos y cómo se miden sus mini-objetivos. */
public enum TipoObjetivo {
    /** Tiene una meta en pesos por mes; sus mini-objetivos son medidos. Es el objetivo de siempre. */
    MONETARIO,
    /** Sin meta en pesos; todos sus mini-objetivos son medidos por el servidor. */
    CON_DATOS,
    /** Sin meta en pesos; todos sus mini-objetivos son acciones que la persona responde. */
    SIN_DATOS,
    /** Sin meta en pesos; mezcla mini-objetivos medidos y manuales. */
    HIBRIDO;

    /** Si tiene una meta en pesos por mes contra la que se compara el resultado del negocio. */
    public boolean tieneMeta() {
        return this == MONETARIO;
    }

    /** El tipo de un objetivo armado con Sami, según los modos de sus mini-objetivos. */
    public static TipoObjetivo de(Collection<MiniObjetivoPropuesto> minis) {
        boolean hayMedidos = minis.stream().anyMatch(MiniObjetivoPropuesto::esMedido);
        boolean hayManuales = minis.stream().anyMatch(MiniObjetivoPropuesto::esManual);
        if (hayMedidos && hayManuales) {
            return HIBRIDO;
        }
        return hayManuales ? SIN_DATOS : CON_DATOS;
    }
}
