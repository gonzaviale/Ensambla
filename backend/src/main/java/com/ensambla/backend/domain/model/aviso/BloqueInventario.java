package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.List;
import java.util.Map;

/**
 * Tabla de inventario del negocio lista para evaluar reglas: sus campos indexados por clave semántica ({@code name},
 * {@code price}, {@code quantity}…) y sus registros. {@code entradasId} es la tabla de entradas del mismo proyecto, si
 * existe.
 */
public record BloqueInventario(
        Bloque bloque,
        BloqueId entradasId,
        Map<String, Campo> camposPorClave,
        List<Registro> registros) {

    /** Valor del registro para la clave, o {@code null} si la tabla no tiene ese campo o el registro no lo cargó. */
    public Object valorDe(Registro registro, String clave) {
        Campo campo = camposPorClave.get(clave);
        return campo == null ? null : registro.getValores().get(campo.getId().toString());
    }
}
