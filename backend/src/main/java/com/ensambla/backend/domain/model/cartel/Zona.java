package com.ensambla.backend.domain.model.cartel;

/** Un rectángulo del cartel, en unidades de dibujo. */
public record Zona(double x, double y, double ancho, double alto) {

    /** La misma zona achicada {@code margen} por cada lado. */
    public Zona interior(double margen) {
        return new Zona(x + margen, y + margen, ancho - 2 * margen, alto - 2 * margen);
    }

    /** La franja que empieza a esta fracción de la altura y ocupa otra fracción. */
    public Zona franja(double inicio, double fraccion) {
        return new Zona(x, y + alto * inicio, ancho, alto * fraccion);
    }
}
