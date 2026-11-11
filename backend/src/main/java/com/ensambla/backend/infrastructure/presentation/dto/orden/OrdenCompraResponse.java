package com.ensambla.backend.infrastructure.presentation.dto.orden;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrdenCompraResponse(
        OrdenCompraId id,
        String clienteTelefono,
        EstadoOrdenCompra estado,
        List<ItemOrdenResponse> items,
        BigDecimal total,
        String motivoRechazo,
        Boolean clienteAvisado,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {
}
