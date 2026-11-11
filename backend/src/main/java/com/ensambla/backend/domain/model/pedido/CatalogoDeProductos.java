package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lo que un comercio puede vender por WhatsApp: los productos de sus inventarios con precio y stock. Las filas de
 * ejemplo que trae una plantilla no son del negocio y no se ofrecen.
 */
public final class CatalogoDeProductos {

    private CatalogoDeProductos() {
    }

    /** @return los productos pedibles, ordenados por nombre */
    public static List<ProductoEnStock> de(List<BloqueInventario> inventarios) {
        return inventarios.stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .filter(registro -> !registro.esDeEjemplo())
                        .map(registro -> ProductoEnStock.de(inventario, registro))
                        .flatMap(Optional::stream))
                .sorted(Comparator.comparing(ProductoEnStock::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
