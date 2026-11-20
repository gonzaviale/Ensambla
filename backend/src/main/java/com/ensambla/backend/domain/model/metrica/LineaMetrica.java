package com.ensambla.backend.domain.model.metrica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Una fila de cualquiera de las tablas, ya leída: lo que hace falta para calcular y repartir. {@code costo} es lo que
 * costó lo vendido (cantidad por el costo actual del producto); sólo lo traen las ventas de productos con costo.
 */
record LineaMetrica(String producto, String cliente, String categoria, LocalDate fecha, BigDecimal importe,
                    BigDecimal unidades, boolean fiado, BigDecimal costo) {

    static final String SIN_CLIENTE = "Sin cliente";

    static final String SIN_NOMBRE = "Sin nombre";

    static final String SIN_CATEGORIA = "Sin categoría";

    private static final String SIN_FECHA = "Sin fecha";

    private static final List<String> DIAS = List.of(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

    LineaMetrica(String producto, String cliente, String categoria, LocalDate fecha, BigDecimal importe,
                 BigDecimal unidades, boolean fiado) {
        this(producto, cliente, categoria, fecha, importe, unidades, fiado, null);
    }

    boolean entraEn(DefinicionMetrica definicion, LocalDate hoy) {
        return definicion.periodo().incluye(fecha, hoy) && cumpleConElFiado(definicion);
    }

    boolean tieneImporte() {
        return importe != null;
    }

    boolean tieneCosto() {
        return costo != null;
    }

    boolean tieneUnidades() {
        return unidades != null;
    }

    String diaDeLaSemana() {
        return fecha == null ? SIN_FECHA : DIAS.get(fecha.getDayOfWeek().ordinal());
    }

    /** El lugar del día en la semana, empezando el lunes; lo que no es un día (sin fecha) va al final. */
    static int lugarEnLaSemana(String dia) {
        int lugar = DIAS.indexOf(dia);
        return lugar < 0 ? DIAS.size() : lugar;
    }

    private boolean cumpleConElFiado(DefinicionMetrica definicion) {
        return !definicion.soloFiado() || fiado;
    }
}
