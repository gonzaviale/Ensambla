package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.time.LocalDate;
import java.util.List;

/** Lo que el negocio le pide a un proveedor (nulo si no se eligió uno), listo para armar el documento. */
public record PedidoReposicion(String negocio, String proveedor, LocalDate fecha, List<ItemReposicion> items) {

    /** @throws BusinessRuleException si no hay ningún producto para pedir */
    public static PedidoReposicion armar(String negocio, String proveedor, LocalDate fecha,
                                         List<ItemReposicion> items) {
        exigirProductos(items);
        return new PedidoReposicion(negocio, proveedorLimpio(proveedor), fecha, items);
    }

    private static void exigirProductos(List<ItemReposicion> items) {
        if (items.isEmpty()) {
            throw new BusinessRuleException("Elegí al menos un producto para el pedido");
        }
    }

    private static String proveedorLimpio(String proveedor) {
        return proveedor == null || proveedor.isBlank() ? null : proveedor.strip();
    }
}
