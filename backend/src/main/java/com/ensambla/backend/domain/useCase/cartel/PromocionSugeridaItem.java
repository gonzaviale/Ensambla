package com.ensambla.backend.domain.useCase.cartel;

import com.ensambla.backend.domain.model.cartel.PromocionSugerida;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;

import java.math.BigDecimal;
import java.util.List;

public record PromocionSugeridaItem(
        TipoDeCartel tipo,
        List<String> productos,
        BigDecimal precioAnterior,
        BigDecimal precioNuevo,
        BigDecimal ahorro,
        BigDecimal descuentoPorcentaje) {

    public static PromocionSugeridaItem de(PromocionSugerida promocion) {
        return new PromocionSugeridaItem(
                promocion.tipo(),
                promocion.productos(),
                promocion.precioAnterior(),
                promocion.precioNuevo(),
                promocion.ahorro(),
                promocion.descuentoPorcentaje());
    }
}
