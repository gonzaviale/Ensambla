package com.ensambla.backend.domain.model.promocion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** El producto que más se vendió en el último período: lo que mejor funciona merece una publicación. */
public class PromocionProductoEstrella implements ReglaPromocion {

    @Override
    public Optional<Promocion> evaluar(ContextoPromocion contexto) {
        return contexto.productos().stream()
                .filter(producto -> contexto.unidadesVendidas(producto).signum() > 0)
                .max(masVendidoPrimero(contexto))
                .map(this::promocionDe);
    }

    /** A igualdad de unidades gana el primero por orden alfabético, para que el resultado no cambie entre corridas. */
    private Comparator<ProductoDeInventario> masVendidoPrimero(ContextoPromocion contexto) {
        return Comparator.comparing(contexto::unidadesVendidas)
                .thenComparing(ProductoDeInventario::nombre, Comparator.reverseOrder());
    }

    private Promocion promocionDe(ProductoDeInventario producto) {
        return new Promocion("estrella", "Tu producto estrella", "Lo que más se vendió este mes",
                List.of(producto.promocionado()));
    }
}
