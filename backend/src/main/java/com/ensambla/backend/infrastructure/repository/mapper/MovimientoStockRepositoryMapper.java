package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import org.springframework.stereotype.Component;

@Component
public class MovimientoStockRepositoryMapper {

    public MovimientoStock toDomain(MovimientoStockEntity entity) {
        return MovimientoStock.builder()
                .id(Identificador.opcional(entity.getId(), MovimientoStockId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .registroId(Identificador.opcional(entity.getRegistroId(), RegistroId::de))
                .tipo(entity.getTipo())
                .cantidad(entity.getCantidad())
                .fecha(entity.getFecha())
                .nota(entity.getNota())
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    public MovimientoStockEntity toEntity(MovimientoStock movimiento) {
        return MovimientoStockEntity.builder()
                .id(Identificador.valorDe(movimiento.getId()))
                .negocioId(Identificador.valorDe(movimiento.getNegocioId()))
                .registroId(Identificador.valorDe(movimiento.getRegistroId()))
                .tipo(movimiento.getTipo())
                .cantidad(movimiento.getCantidad())
                .fecha(movimiento.getFecha())
                .nota(movimiento.getNota())
                .creadoEn(movimiento.getCreadoEn())
                .build();
    }
}
