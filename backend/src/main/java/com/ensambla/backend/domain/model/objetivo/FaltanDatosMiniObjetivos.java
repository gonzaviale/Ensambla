package com.ensambla.backend.domain.model.objetivo;

/** Por qué un objetivo no tiene sus 3 mini-objetivos en curso. */
public enum FaltanDatosMiniObjetivos {
    /** El resultado del negocio ya llega a la meta: no hace falta proponer nada. */
    OBJETIVO_ALCANZADO,
    /** Todavía no hay ventas, costos o gastos cargados con los que proponer más. */
    POCOS_DATOS
}
