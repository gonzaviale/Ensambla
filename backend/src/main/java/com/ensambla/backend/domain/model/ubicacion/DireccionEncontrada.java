package com.ensambla.backend.domain.model.ubicacion;

import java.math.BigDecimal;

/** {@code localidad} es la que el proveedor reconoce para la dirección; puede faltar. */
public record DireccionEncontrada(String direccion, BigDecimal latitud, BigDecimal longitud, String localidad) {
}
