package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.List;
import java.util.Map;

/**
 * Tabla de ventas del negocio lista para evaluar reglas: sus campos indexados por clave semántica ({@code product},
 * {@code quantity}, {@code price}, {@code date}, {@code customer}…) y sus registros. Cada registro es la venta de un
 * producto; no hay ticket, así que "la misma venta" es el mismo cliente el mismo día.
 */
public record BloqueVentas(
        Bloque bloque,
        Map<String, Campo> camposPorClave,
        List<Registro> registros) {

    /** Valor del registro para la clave, o {@code null} si la tabla no tiene ese campo o el registro no lo cargó. */
    public Object valorDe(Registro registro, String clave) {
        Campo campo = camposPorClave.get(clave);
        return campo == null ? null : registro.getValores().get(campo.getId().toString());
    }
}
