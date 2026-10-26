package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;

import java.math.BigDecimal;
import java.util.Map;

public record TotalesItem(long cantidad, Map<CampoId, BigDecimal> sumas) {

    public static TotalesItem de(TotalesRegistros totales) {
        return new TotalesItem(totales.cantidad(), totales.sumas());
    }
}
