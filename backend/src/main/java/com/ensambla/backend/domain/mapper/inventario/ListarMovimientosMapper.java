package com.ensambla.backend.domain.mapper.inventario;

import com.ensambla.backend.domain.dto.inventario.MovimientoItem;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListarMovimientosMapper {

    private final MovimientoItemMapper movimientoItemMapper;

    /** {@code productos} son las filas movidas y {@code campos} los del bloque: de ahí sale el nombre de cada una. */
    public List<MovimientoItem> toResponse(List<MovimientoStock> movimientos, List<Registro> productos,
                                           List<Campo> campos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(campos);
        Map<UUID, String> nombres = productos.stream()
                .map(etiquetador)
                .collect(Collectors.toMap(Referencia::id, Referencia::etiqueta));
        return movimientos.stream()
                .map(movimiento -> movimientoItemMapper.toItem(
                        movimiento, nombres.getOrDefault(movimiento.getRegistroId(), Referencia.SIN_NOMBRE)))
                .toList();
    }
}
