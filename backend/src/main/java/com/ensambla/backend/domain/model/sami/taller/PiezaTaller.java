package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Una pieza del sistema que se está armando en el taller (inventario, ventas, clientes…).
 *
 * @param id        identificador que le puso el taller; las piezas que se suman en el mismo turno todavía no lo tienen
 * @param tipo      tipo del catálogo de componentes ({@link CatalogoComponentes})
 * @param seccion   nombre de la sección donde está
 * @param registros los registros reales que se cargaron en este armado (sin las filas de ejemplo), para poder
 *                  corregirlos
 * @param nombres   si otra pieza se vincula con esta (clientes, productos), los nombres que ya tiene, también los
 *                  guardados: Sami los usa tal cual en vez de inventar otro
 */
public record PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos,
                          List<RegistroTaller> registros, List<String> nombres) {

    public PiezaTaller {
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
        registros = List.copyOf(Objects.requireNonNullElse(registros, List.of()));
        nombres = List.copyOf(Objects.requireNonNullElse(nombres, List.of()));
    }

    /** Una pieza sin nombres existentes. */
    public PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos,
                       List<RegistroTaller> registros) {
        this(id, tipo, nombre, seccion, campos, registros, List.of());
    }

    /** Una pieza sin registros ni nombres existentes. */
    public PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos) {
        this(id, tipo, nombre, seccion, campos, List.of());
    }
}
