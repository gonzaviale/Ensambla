package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** El cobro con QR tal como se muestra: el estado ya tiene en cuenta si venció. */
public record CobroQrItem(CobroQrId id, BigDecimal importe, String alias, String titular,
                          String estado, OffsetDateTime venceEn) {

    public static CobroQrItem de(CobroQr cobro, OffsetDateTime ahora) {
        return new CobroQrItem(cobro.id(), cobro.importe(), cobro.destino().alias(), cobro.destino().titular(),
                cobro.estadoEn(ahora).name(), cobro.venceEn());
    }
}
