package com.ensambla.backend.infrastructure.presentation.mapper.inventario;

import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoRegistradoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.MovimientoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.PaginaStockResponse;
import com.ensambla.backend.infrastructure.presentation.dto.inventario.StockResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class InventarioPresentationMapper {

    public RegistrarMovimientoRequest toRegistrarRequest(UUID usuarioId, UUID bloqueId, MovimientoRequest request) {
        return new RegistrarMovimientoRequest(
                usuarioId,
                bloqueId,
                request.registroId(),
                request.tipo(),
                request.cantidad(),
                request.fecha(),
                request.nota());
    }

    public ListarMovimientosRequest toListarMovimientosRequest(UUID usuarioId, UUID bloqueId, UUID registroId) {
        return new ListarMovimientosRequest(usuarioId, bloqueId, registroId);
    }

    public ConsultarStockRequest toStockRequest(UUID usuarioId, UUID bloqueId, String texto, Integer pagina,
                                                Integer tamanio) {
        return new ConsultarStockRequest(usuarioId, bloqueId, texto, pagina, tamanio);
    }

    public MovimientoRegistradoResponse toResponse(RegistrarMovimientoResponse response) {
        return new MovimientoRegistradoResponse(toResponse(response.movimiento()), response.stockActual());
    }

    public List<MovimientoResponse> toResponse(List<MovimientoItem> movimientos) {
        return movimientos.stream().map(this::toResponse).toList();
    }

    public PaginaStockResponse toResponse(ConsultarStockResponse response) {
        return new PaginaStockResponse(
                response.contenido().stream()
                        .map(item -> new StockResponse(item.registroId(), item.producto(), item.stock()))
                        .toList(),
                response.pagina(),
                response.tamanio(),
                response.totalElementos(),
                response.totalPaginas(),
                response.pagina() == 0,
                response.pagina() >= response.totalPaginas() - 1);
    }

    private MovimientoResponse toResponse(MovimientoItem item) {
        return new MovimientoResponse(
                item.id(),
                item.registroId(),
                item.producto(),
                item.tipo(),
                item.cantidad(),
                item.fecha(),
                item.nota(),
                item.creadoEn());
    }
}
