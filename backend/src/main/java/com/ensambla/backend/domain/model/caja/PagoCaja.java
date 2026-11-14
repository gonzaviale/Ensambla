package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.math.BigDecimal;

public record PagoCaja(MedioPagoCaja medio, BigDecimal importe) {

    public PagoCaja {
        if (medio == MedioPagoCaja.MIXTO || importe.signum() <= 0 || importe.scale() > 2) {
            throw new BusinessRuleException("Cada pago necesita un medio y un importe mayor a cero con hasta dos decimales");
        }
    }
}
