package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import org.springframework.stereotype.Component;

@Component
public class MovimientoStockRepositoryMapper {

    public MovimientoStock toDomain(MovimientoStockEntity entity) {
        return MovimientoStock.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .registroId(entity.getRegistroId())
                .tipo(entity.getTipo())
                .cantidad(entity.getCantidad())
                .fecha(entity.getFecha())
                .nota(entity.getNota())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    public MovimientoStockEntity toEntity(MovimientoStock movimiento) {
        return MovimientoStockEntity.builder()
                .id(movimiento.getId())
                .negocioId(movimiento.getNegocioId())
                .registroId(movimiento.getRegistroId())
                .tipo(movimiento.getTipo())
                .cantidad(movimiento.getCantidad())
                .fecha(movimiento.getFecha())
                .nota(movimiento.getNota())
                .creadoEn(movimiento.getCreadoEn())
                .build();
    }
}
