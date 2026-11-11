package com.ensambla.backend.domain.useCase.orden;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Una orden de compra tal como la ve el dueño en la app.
 *
 * @param clienteTelefono el WhatsApp del cliente, para que el dueño sepa quién pidió
 * @param motivoRechazo   por qué la rechazó; null si no está rechazada o no lo explicó
 */
public record OrdenItem(
        OrdenCompraId id,
        String clienteTelefono,
        EstadoOrdenCompra estado,
        List<LineaOrdenItem> items,
        BigDecimal total,
        String motivoRechazo,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {

    public static OrdenItem de(OrdenCompra orden, String clienteTelefono) {
        return new OrdenItem(
                orden.getId(),
                clienteTelefono,
                orden.getEstado(),
                orden.getItems().stream().map(LineaOrdenItem::de).toList(),
                orden.getTotal(),
                orden.getMotivoRechazo(),
                orden.getCreadoEn(),
                orden.getActualizadoEn());
    }
}
