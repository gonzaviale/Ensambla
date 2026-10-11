package com.ensambla.backend.domain.dto.registro;

import com.ensambla.backend.domain.model.registro.TotalesRegistros;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record TotalesItem(long cantidad, Map<UUID, BigDecimal> sumas) {

    public static TotalesItem de(TotalesRegistros totales) {
        return new TotalesItem(totales.cantidad(), totales.sumas());
    }
}
