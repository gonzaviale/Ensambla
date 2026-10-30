package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;

/**
 * Dos productos que el mismo cliente se llevó el mismo día. Siempre vienen ordenados ({@code productoA} antes que
 * {@code productoB} por su texto) para que el par sea el mismo sin importar cuál se anotó primero.
 *
 * @param veces cuántas canastas (cliente y día) tenían a los dos
 */
public record ParProductos(RegistroId productoA, RegistroId productoB, int veces) {

    public static ParProductos de(RegistroId uno, RegistroId otro, int veces) {
        return uno.toString().compareTo(otro.toString()) <= 0
                ? new ParProductos(uno, otro, veces)
                : new ParProductos(otro, uno, veces);
    }
}
