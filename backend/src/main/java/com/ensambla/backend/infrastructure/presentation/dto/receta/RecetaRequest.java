package com.ensambla.backend.infrastructure.presentation.dto.receta;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body de guardar la receta: reemplaza la anterior; sin líneas, la quita. La columna del precio (numérica) y cada línea
 * (insumo, cantidad y unidad opcional) los valida el dominio, que responde 400 con un mensaje por dato.
 */
public record RecetaRequest(
        CampoId campoPrecioId,
        List<Linea> lineas
) {

    public record Linea(RegistroId insumoId, BigDecimal cantidad, String unidad) {
    }
}
