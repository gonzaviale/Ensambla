package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;

/**
 * Algo para promocionar: un título, el gancho que lo explica y los productos. {@code clave} dice de qué idea salió
 * («estrella», «liquidar»).
 */
public record Promocion(String clave, String titulo, String gancho, List<ProductoPromocionado> productos) {

    private static final int MAXIMO_DE_PRODUCTOS = 12;

    /**
     * @throws BusinessRuleException si no hay productos o hay más de los que entran en una publicación
     */
    public static Promocion armar(String clave, String titulo, String gancho, List<ProductoPromocionado> productos) {
        exigirProductos(productos);
        return new Promocion(clave, titulo, gancho, productos);
    }

    private static void exigirProductos(List<ProductoPromocionado> productos) {
        if (productos.isEmpty()) {
            throw new BusinessRuleException("Elegí al menos un producto para promocionar");
        }
        if (productos.size() > MAXIMO_DE_PRODUCTOS) {
            throw new BusinessRuleException("Una promoción lleva hasta " + MAXIMO_DE_PRODUCTOS + " productos");
        }
    }
}
