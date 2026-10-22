package com.ensambla.backend.domain.model.sami.taller;

/**
 * Un vínculo entre dos piezas de un kit, por su nombre en el kit: {@code desde} suma un dato {@code etiqueta} que
 * elige una fila de {@code hacia}.
 */
public record ConexionKit(String desde, String hacia, String etiqueta) {
}
