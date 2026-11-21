package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CierreCajaItem(CierreCajaId id, ResumenCajaItem resumen, BigDecimal fondoInicial,
                             BigDecimal entradas, BigDecimal retiros, BigDecimal esperado,
                             BigDecimal contado, BigDecimal diferencia, String nota, OffsetDateTime cerradoEn) {

    public static CierreCajaItem de(CierreCaja cierre) {
        return new CierreCajaItem(cierre.id(), ResumenCajaItem.de(cierre.resumen()), cierre.fondoInicial(),
                cierre.entradas(), cierre.retiros(), cierre.esperado(), cierre.contado(), cierre.diferencia(),
                cierre.nota(), cierre.cerradoEn());
    }
}
