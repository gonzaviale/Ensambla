package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;

/**
 * Campo de un bloque que el negocio ya tiene, tal como se le muestra a la IA: nombre, tipo y opciones.
 *
 * @param bloqueRelacionado nombre de la tabla a la que apunta un campo RELACION; {@code null} en los demás tipos
 */
public record CampoContexto(String nombre, TipoCampo tipo, List<String> opciones, String bloqueRelacionado) {

    public CampoContexto {
        opciones = List.copyOf(opciones);
    }

    public CampoContexto(String nombre, TipoCampo tipo, List<String> opciones) {
        this(nombre, tipo, opciones, null);
    }
}
