package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Una pieza del sistema que se está armando en el taller (inventario, ventas, clientes…).
 *
 * @param id      identificador que le puso el taller; las piezas que se suman en el mismo turno todavía no lo tienen
 * @param tipo    tipo del catálogo de componentes ({@link CatalogoComponentes})
 * @param seccion nombre de la sección donde está
 * @param filas   las primeras filas, para poder cambiar o quitar un registro; vacío si no se mandaron
 */
public record PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos,
                          List<FilaTaller> filas) {

    public PiezaTaller {
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
        filas = List.copyOf(Objects.requireNonNullElse(filas, List.of()));
    }

    public PiezaTaller(String id, String tipo, String nombre, String seccion, List<CampoTaller> campos) {
        this(id, tipo, nombre, seccion, campos, List.of());
    }
}
