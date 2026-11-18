package com.ensambla.backend.infrastructure.presentation.dto.caja;

import com.ensambla.backend.domain.model.registro.RegistroId;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CuentaClienteCajaWebResponse(RegistroId clienteId, String clienteNombre, BigDecimal saldo,
        int cantidadPendientes, OffsetDateTime pendienteDesde, List<CobrarVentaWebResponse> ventas,
        List<CobroCajaWebResponse> cobros) {
}
