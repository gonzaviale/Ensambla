package com.ensambla.backend.domain.mapper.inventario;

import com.ensambla.backend.domain.dto.inventario.RegistrarMovimientoResponse;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class RegistrarMovimientoMapper {

    private final MovimientoItemMapper movimientoItemMapper;

    public RegistrarMovimientoResponse toResponse(MovimientoStock movimiento, String producto, BigDecimal stock) {
        return new RegistrarMovimientoResponse(movimientoItemMapper.toItem(movimiento, producto), stock);
    }
}
