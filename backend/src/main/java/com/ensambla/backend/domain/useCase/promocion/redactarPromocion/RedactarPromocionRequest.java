package com.ensambla.backend.domain.useCase.promocion.redactarPromocion;

import com.ensambla.backend.domain.model.promocion.ProductoPromocionado;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.List;

/**
 * La promoción que la persona eligió (con los productos que dejó) y el canal donde la va a compartir; la clave de la
 * promoción es la de la idea de la que salió, para saber qué se publicó.
 */
public record RedactarPromocionRequest(
        UsuarioId usuarioId,
        String canal,
        String clave,
        String titulo,
        String gancho,
        List<ProductoARedactar> productos) {

    /** Un producto con el precio al que se anuncia. */
    public record ProductoARedactar(String nombre, BigDecimal precio) {
    }

    /** @throws com.ensambla.backend.domain.exception.BusinessRuleException si no hay productos o hay demasiados */
    public Promocion promocion() {
        return Promocion.armar(clave, titulo, gancho, productos.stream()
                .map(producto -> new ProductoPromocionado(producto.nombre(), producto.precio()))
                .toList());
    }
}
