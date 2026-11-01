package com.ensambla.backend.infrastructure.presentation.dto.receta;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;

public record RecetaResponse(
        BloqueId bloqueInsumosId,
        CampoId campoPrecioId,
        List<Linea> lineas,
        BigDecimal costo,
        boolean incompleta
) {

    public record Linea(
            RegistroId insumoId,
            String insumo,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
    }
}
