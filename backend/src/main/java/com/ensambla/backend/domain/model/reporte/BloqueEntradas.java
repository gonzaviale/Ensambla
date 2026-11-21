package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.List;
import java.util.Map;

/**
 * Tabla de entradas del negocio (compras y reposición de mercadería): sus campos indexados por clave semántica
 * ({@code product}, {@code quantity}, {@code date}) y sus registros. No guarda el precio de compra: se valúa con el
 * costo que tiene hoy cada producto en el inventario.
 */
public record BloqueEntradas(
        Bloque bloque,
        Map<String, Campo> camposPorClave,
        List<Registro> registros) {

    /** Valor del registro para la clave, o {@code null} si la tabla no tiene ese campo o el registro no lo cargó. */
    public Object valorDe(Registro registro, String clave) {
        Campo campo = camposPorClave.get(clave);
        return campo == null ? null : registro.getValores().get(campo.getId().toString());
    }
}
