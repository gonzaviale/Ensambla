package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Un movimiento con el nombre del producto (la etiqueta de su fila). */
public record MovimientoItem(
        MovimientoStockId id,
        RegistroId registroId,
        String producto,
        String tipo,
        BigDecimal cantidad,
        LocalDate fecha,
        String nota,
        OffsetDateTime creadoEn) {

    /** {@code producto} es la etiqueta de la fila movida. */
    public static MovimientoItem de(MovimientoStock movimiento, String producto) {
        return new MovimientoItem(
                movimiento.getId(),
                movimiento.getRegistroId(),
                producto,
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getFecha(),
                movimiento.getNota(),
                movimiento.getCreadoEn());
    }

    /** {@code productos} son las filas movidas y {@code campos} los del bloque: de ahí sale el nombre de cada una. */
    public static List<MovimientoItem> de(List<MovimientoStock> movimientos, List<Registro> productos,
                                          List<Campo> campos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(campos);
        Map<RegistroId, String> nombres = productos.stream()
                .map(etiquetador)
                .collect(Collectors.toMap(Referencia::id, Referencia::etiqueta));
        return movimientos.stream()
                .map(movimiento -> de(
                        movimiento, nombres.getOrDefault(movimiento.getRegistroId(), Referencia.SIN_NOMBRE)))
                .toList();
    }
}
