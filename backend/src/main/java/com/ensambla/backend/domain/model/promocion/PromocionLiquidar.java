package com.ensambla.backend.domain.model.promocion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Productos con stock que no se vendieron en el último período: conviene darles un empujón antes de que se frenen. */
public class PromocionLiquidar implements ReglaPromocion {

    private static final int MAXIMO_DE_PRODUCTOS = 3;

    @Override
    public Optional<Promocion> evaluar(ContextoPromocion contexto) {
        if (!contexto.hayVentasAnotadas()) {
            return Optional.empty();
        }
        List<ProductoPromocionado> parados = paradosDe(contexto);
        return parados.isEmpty() ? Optional.empty() : Optional.of(promocionDe(parados));
    }

    /** Los que más stock tienen primero: es lo que más plata deja quieta. */
    private List<ProductoPromocionado> paradosDe(ContextoPromocion contexto) {
        return contexto.productos().stream()
                .filter(producto -> producto.cantidad().signum() > 0)
                .filter(producto -> contexto.unidadesVendidas(producto).signum() == 0)
                .sorted(Comparator.comparing(ProductoDeInventario::cantidad).reversed()
                        .thenComparing(ProductoDeInventario::nombre))
                .limit(MAXIMO_DE_PRODUCTOS)
                .map(ProductoDeInventario::promocionado)
                .toList();
    }

    private Promocion promocionDe(List<ProductoPromocionado> parados) {
        return new Promocion("liquidar", "Liquidá lo que no se mueve",
                "Productos que tenés en stock y no se vendieron este mes", parados);
    }
}
