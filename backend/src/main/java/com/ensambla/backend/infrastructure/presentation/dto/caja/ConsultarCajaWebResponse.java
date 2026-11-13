package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CierreCajaId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ConsultarCajaWebResponse(LocalDate fecha, ResumenCajaWebResponse dia, ResumenCajaWebResponse abierta,
                                       List<CierreListaWebResponse> cierres, int pagina, boolean hayMas) {

    public record CierreListaWebResponse(CierreCajaId id, OffsetDateTime desde, OffsetDateTime cerradoEn,
                                         BigDecimal total, int cantidadVentas, BigDecimal diferencia) {
    }
}
