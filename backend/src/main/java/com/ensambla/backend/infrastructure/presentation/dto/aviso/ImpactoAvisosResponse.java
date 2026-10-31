package com.ensambla.backend.infrastructure.presentation.dto.aviso;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ImpactoAvisosResponse(
        long aplicados,
        long descartados,
        BigDecimal ingresoSemanalDelta,
        List<Medido> medidos) {

    public record Medido(
            AvisoId avisoId,
            TipoAviso tipo,
            String titulo,
            OffsetDateTime aplicadoEn,
            int ventanaDias,
            BigDecimal unidadesPorDiaAntes,
            BigDecimal unidadesPorDiaDespues,
            BigDecimal ingresoPorDiaAntes,
            BigDecimal ingresoPorDiaDespues,
            BigDecimal ingresoSemanalDelta) {
    }
}
