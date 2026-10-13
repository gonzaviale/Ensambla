package com.ensambla.backend.domain.mapper.inventario;

import com.ensambla.backend.domain.dto.inventario.ConsultarStockResponse;
import com.ensambla.backend.domain.dto.inventario.StockItem;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ConsultarStockMapper {

    /** {@code stock} trae un valor por cada fila de la página; {@code campos} son los del bloque. */
    public ConsultarStockResponse toResponse(Pagina<Registro> productos, Map<UUID, BigDecimal> stock,
                                             List<Campo> campos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(campos);
        return new ConsultarStockResponse(
                productos.contenido().stream()
                        .map(producto -> new StockItem(
                                producto.getId(), etiquetador.apply(producto).etiqueta(), stock.get(producto.getId())))
                        .toList(),
                productos.numero(),
                productos.tamanio(),
                productos.totalElementos(),
                productos.totalPaginas());
    }
}
