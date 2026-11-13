package com.ensambla.backend.domain.useCase.caja.consultarCaja;

import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.useCase.caja.ResumenCajaItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ConsultarCajaResponse(LocalDate fecha, ResumenCajaItem dia, ResumenCajaItem abierta,
                                     List<CierreListaItem> cierres, int pagina, boolean hayMas) {

    public record CierreListaItem(CierreCajaId id, OffsetDateTime desde, OffsetDateTime cerradoEn,
                                  BigDecimal total, int cantidadVentas, BigDecimal diferencia) {

        public static CierreListaItem de(CierreCaja cierre) {
            return new CierreListaItem(cierre.id(), cierre.resumen().desde(), cierre.cerradoEn(),
                    cierre.resumen().total(), cierre.resumen().ventas().size(), cierre.diferencia());
        }
    }

    public static ConsultarCajaResponse de(LocalDate fecha, ResumenCaja dia, ResumenCaja abierta,
            List<CierreCaja> cierres, int pagina) {
        return new ConsultarCajaResponse(fecha, ResumenCajaItem.de(dia), ResumenCajaItem.de(abierta),
                cierres.stream().map(CierreListaItem::de).toList(), pagina, cierres.size() == 20);
    }
}
