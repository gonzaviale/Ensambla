package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CierreCajaId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CierreCajaWebResponse(CierreCajaId id, ResumenCajaWebResponse resumen, BigDecimal fondoInicial,
                                    BigDecimal entradas, BigDecimal retiros, BigDecimal esperado,
                                    BigDecimal contado, BigDecimal diferencia, String nota, OffsetDateTime cerradoEn) {
}
