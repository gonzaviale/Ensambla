package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.List;
import java.util.Map;

/**
 * Tabla de gastos del negocio lista para el resumen: sus campos indexados por clave semántica ({@code name},
 * {@code date}, {@code total}, {@code category}) y sus registros.
 */
public record BloqueGastos(
        Bloque bloque,
        Map<String, Campo> camposPorClave,
        List<Registro> registros) {

    /** Valor del registro para la clave, o {@code null} si la tabla no tiene ese campo o el registro no lo cargó. */
    public Object valorDe(Registro registro, String clave) {
        Campo campo = camposPorClave.get(clave);
        return campo == null ? null : registro.getValores().get(campo.getId().toString());
    }

    /** Sólo se pueden sumar los gastos de una tabla que tiene el campo del importe. */
    public boolean tieneImporte() {
        return camposPorClave.containsKey("total");
    }
}
