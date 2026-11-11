package com.ensambla.backend.infrastructure.presentation.mapper.orden;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.orden.LineaOrdenItem;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrdenRequest;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenesRequest;
import com.ensambla.backend.infrastructure.presentation.dto.orden.CambiarEstadoOrdenWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.orden.ItemOrdenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.orden.OrdenCompraResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrdenPresentationMapper {

    public ListarOrdenesRequest toListarRequest(UsuarioId usuarioId, EstadoOrdenCompra estado) {
        return new ListarOrdenesRequest(usuarioId, estado);
    }

    public CambiarEstadoOrdenRequest toCambiarEstadoRequest(UsuarioId usuarioId, OrdenCompraId ordenId,
                                                            CambiarEstadoOrdenWebRequest request) {
        return new CambiarEstadoOrdenRequest(usuarioId, ordenId, request.estado(), request.motivo());
    }

    public List<OrdenCompraResponse> toResponse(List<OrdenItem> ordenes) {
        return ordenes.stream().map(this::toResponse).toList();
    }

    public OrdenCompraResponse toResponse(OrdenItem orden) {
        return new OrdenCompraResponse(
                orden.id(),
                orden.clienteTelefono(),
                orden.estado(),
                orden.items().stream().map(this::toResponse).toList(),
                orden.total(),
                orden.motivoRechazo(),
                orden.clienteAvisado(),
                orden.creadoEn(),
                orden.actualizadoEn());
    }

    private ItemOrdenResponse toResponse(LineaOrdenItem linea) {
        return new ItemOrdenResponse(linea.nombre(), linea.cantidad(), linea.precioUnitario(), linea.subtotal());
    }
}
