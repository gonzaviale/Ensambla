package com.ensambla.backend.domain.model.plan;

/** Qué cupo gratuito mensual se gasta: cada uno cuenta aparte, así una pantalla no deja sin lugar a otra. */
public enum TipoContenidoPlan {

    AVISOS,
    /** Mi zona, pestaña Precios. */
    ZONA,
    /** Mi zona, pestaña Qué se vende cerca. */
    ZONA_CERCA,
    /** Mi zona, pestaña Otra zona. */
    ZONA_OTRA
}
