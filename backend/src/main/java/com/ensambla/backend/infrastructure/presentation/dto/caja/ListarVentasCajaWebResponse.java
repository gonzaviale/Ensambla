package com.ensambla.backend.infrastructure.presentation.dto.caja;

import java.util.List;

public record ListarVentasCajaWebResponse(List<CobrarVentaWebResponse> ventas, List<CobroCajaWebResponse> cobros) {
}
