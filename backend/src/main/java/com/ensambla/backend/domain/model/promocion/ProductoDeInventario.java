package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Optional;

/** Un producto del inventario que se puede promocionar: tiene nombre y precio. Sin cantidad cargada cuenta como cero. */
public record ProductoDeInventario(RegistroId id, String nombre, BigDecimal precio, BigDecimal cantidad) {

    private static final String CLAVE_NOMBRE = "name";
    private static final String CLAVE_PRECIO = "price";
    private static final String CLAVE_CANTIDAD = "quantity";

    /** El producto de la fila, o vacío si no tiene nombre o no tiene un precio mayor a cero. */
    public static Optional<ProductoDeInventario> de(BloqueInventario inventario, Registro registro) {
        Optional<String> nombre = nombreDe(inventario.valorDe(registro, CLAVE_NOMBRE));
        Optional<BigDecimal> precio = numeroDe(inventario.valorDe(registro, CLAVE_PRECIO))
                .filter(importe -> importe.signum() > 0);
        BigDecimal cantidad = numeroDe(inventario.valorDe(registro, CLAVE_CANTIDAD)).orElse(BigDecimal.ZERO);
        return nombre.flatMap(texto -> precio.map(importe ->
                new ProductoDeInventario(registro.getId(), texto, importe, cantidad)));
    }

    public ProductoPromocionado promocionado() {
        return new ProductoPromocionado(nombre, precio);
    }

    private static Optional<String> nombreDe(Object valor) {
        return Optional.ofNullable(valor)
                .filter(String.class::isInstance)
                .map(texto -> ((String) texto).strip())
                .filter(texto -> !texto.isEmpty());
    }

    private static Optional<BigDecimal> numeroDe(Object valor) {
        return Optional.ofNullable(valor)
                .filter(Number.class::isInstance)
                .map(numero -> new BigDecimal(numero.toString()));
    }
}
