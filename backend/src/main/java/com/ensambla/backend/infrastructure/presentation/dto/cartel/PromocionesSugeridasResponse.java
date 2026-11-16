package com.ensambla.backend.infrastructure.presentation.dto.cartel;

import com.ensambla.backend.domain.model.cartel.TipoDeCartel;

import java.math.BigDecimal;
import java.util.List;

public record PromocionesSugeridasResponse(List<PromocionResponse> promociones) {

    public record PromocionResponse(
            TipoDeCartel tipo,
            List<String> productos,
            BigDecimal precioAnterior,
            BigDecimal precioNuevo,
            BigDecimal ahorro,
            BigDecimal descuentoPorcentaje) {
    }
}
