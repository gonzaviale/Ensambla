package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CobroCajaWebResponse(CobroCajaId id, RegistroId clienteId, String clienteNombre,
                                   List<AplicacionWebResponse> aplicaciones,
                                   BigDecimal importe, OffsetDateTime creadoEn) {

    public record AplicacionWebResponse(VentaCajaId ventaId, List<PagoWebResponse> pagos, BigDecimal importe) {
    }

    public record PagoWebResponse(String medio, BigDecimal importe) {
    }
}
