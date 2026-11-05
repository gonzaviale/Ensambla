package com.ensambla.backend.domain.model.aviso;

import java.util.Set;

/** Cuántos avisos hay para revisar precios: frente a la zona, de productos parados o muy vendidos, o tras un aumento. */
public class SeccionAvisosDePrecio extends SeccionPorCantidad {

    private static final Set<TipoAviso> TIPOS_DE_PRECIO = Set.of(
            TipoAviso.PRECIO_ZONA, TipoAviso.ESTANCADO, TipoAviso.MUY_VENDIDO, TipoAviso.REVISAR_PRECIO);

    @Override
    protected boolean corresponde(Aviso aviso) {
        return TIPOS_DE_PRECIO.contains(aviso.getTipo());
    }

    @Override
    protected String descripcion(long cantidad) {
        return cantidadConUnidad(cantidad, "aviso de precio", "avisos de precio");
    }
}
