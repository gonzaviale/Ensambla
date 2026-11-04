package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;

import java.util.List;

/**
 * Cada pantalla de Mi zona pide cosas distintas de la misma zona, así que define qué se ve primero y qué cupo gratuito
 * gasta: lo que ya tengo (Precios), lo que todavía no tengo (Qué se vende cerca) o lo más vendido sin mirar mi
 * inventario (Otra zona).
 */
public enum VistaZona {

    PRECIOS(TipoContenidoPlan.ZONA),
    CERCA(TipoContenidoPlan.ZONA_CERCA),
    OTRA(TipoContenidoPlan.ZONA_OTRA);

    private final TipoContenidoPlan tipoDeContenido;

    VistaZona(TipoContenidoPlan tipoDeContenido) {
        this.tipoDeContenido = tipoDeContenido;
    }

    /** El cupo gratuito que gasta esta pantalla. */
    public TipoContenidoPlan tipoDeContenido() {
        return tipoDeContenido;
    }

    /** Si el orden depende de los productos del negocio (hace falta mirar su inventario). */
    public boolean usaInventario() {
        return this != OTRA;
    }

    /** La zona con los productos en el orden en que esta pantalla los quiere ver (y desbloquear). */
    public AnalisisZona ordenar(AnalisisZona analisis, List<String> nombresPropios) {
        return switch (this) {
            case PRECIOS -> analisis.conLosPropiosPrimero(nombresPropios);
            case CERCA -> analisis.ordenadoPorVentas().conLosQueFaltanPrimero(nombresPropios);
            case OTRA -> analisis.ordenadoPorVentas();
        };
    }
}
