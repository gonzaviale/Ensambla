package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Un renglón de la lista de compra: un producto real del comercio con su cantidad y su precio. Nada sale de otro lado
 * que del inventario del comercio.
 *
 * @param producto         el producto elegido
 * @param cantidad         cuántas unidades; nunca más que el stock
 * @param limitadaPorStock si pidió más de lo que hay y se le dejó lo que hay
 * @param alternativas     otros productos del comercio que podría preferir (otra marca), para cambiarlo
 */
public record LineaDeCompra(ProductoEnStock producto, BigDecimal cantidad, boolean limitadaPorStock,
                            List<ProductoEnStock> alternativas) {

    public LineaDeCompra {
        alternativas = List.copyOf(alternativas);
    }

    /** Sin cantidad (o con una que no sirve) se pide una unidad; si pidió más de lo que hay, se deja el stock. */
    public static LineaDeCompra de(ProductoEnStock producto, BigDecimal pedida, List<ProductoEnStock> alternativas) {
        BigDecimal cantidad = pedida == null || pedida.signum() <= 0 ? BigDecimal.ONE : pedida;
        return conStock(producto, cantidad, alternativas);
    }

    /** El mismo producto pedido dos veces es un solo renglón con las cantidades sumadas. */
    public LineaDeCompra sumar(LineaDeCompra otra) {
        return conStock(producto, cantidad.add(otra.cantidad), alternativas);
    }

    /**
     * El mismo renglón con otra marca: la misma cantidad (con el tope del stock de la nueva) y, entre las alternativas,
     * la que se dejó, así puede volver atrás.
     */
    public LineaDeCompra cambiarPor(ProductoEnStock nuevo) {
        List<ProductoEnStock> otras = Stream.concat(
                alternativas.stream().filter(alternativa -> !alternativa.id().equals(nuevo.id())),
                Stream.of(producto)).toList();
        return conStock(nuevo, cantidad, otras);
    }

    /** Si el producto sigue en el catálogo con el mismo precio y con stock para la cantidad que se confirma. */
    public boolean sigueVigente(Map<RegistroId, ProductoEnStock> catalogo) {
        ProductoEnStock actual = catalogo.get(producto.id());
        return actual != null && actual.precio().compareTo(producto.precio()) == 0
                && actual.stock().compareTo(cantidad) >= 0;
    }

    public BigDecimal subtotal() {
        return producto.precio().multiply(cantidad);
    }

    private static LineaDeCompra conStock(ProductoEnStock producto, BigDecimal cantidad,
                                          List<ProductoEnStock> alternativas) {
        boolean excede = cantidad.compareTo(producto.stock()) > 0;
        return new LineaDeCompra(producto, excede ? producto.stock() : cantidad, excede, alternativas);
    }
}
