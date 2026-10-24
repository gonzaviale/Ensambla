package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public record ConsultarStockResponse(
        List<StockItem> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    /** {@code stock} trae un valor por cada fila de la página; {@code campos} son los del bloque. */
    public static ConsultarStockResponse de(Pagina<Registro> productos, Map<UUID, BigDecimal> stock,
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
