package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CobrarVentaWebResponse(VentaCajaId id, List<LineaWebResponse> lineas, String medio,
                                     BigDecimal total, BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn) {

    public record LineaWebResponse(RegistroId productoId, String nombre, String modo, BigDecimal cantidad,
                                   BigDecimal precio, BigDecimal importe) {
    }
}
