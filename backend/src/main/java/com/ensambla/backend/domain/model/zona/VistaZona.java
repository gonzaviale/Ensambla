package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;

import java.util.List;

/**
 * Cada pantalla de Mi zona pide cosas distintas de la misma zona, así que define qué se ve primero y qué cupo gratuito
 * gasta: lo que ya tengo (Precios), lo que todavía no tengo (Qué se vende cerca) o lo más vendido sin mirar mi
 * inventario (Otra zona).
 */
public enum VistaZona {

    PRECIOS(TipoContenidoPlan.ZONA, true) {
        @Override
        public AnalisisZona ordenar(AnalisisZona analisis, List<String> nombresPropios) {
            return analisis.conLosPropiosPrimero(nombresPropios);
        }
    },
    CERCA(TipoContenidoPlan.ZONA_CERCA, true) {
        @Override
        public AnalisisZona ordenar(AnalisisZona analisis, List<String> nombresPropios) {
            return analisis.ordenadoPorVentas().conLosQueFaltanPrimero(nombresPropios);
        }
    },
    OTRA(TipoContenidoPlan.ZONA_OTRA, false) {
        @Override
        public AnalisisZona ordenar(AnalisisZona analisis, List<String> nombresPropios) {
            return analisis.ordenadoPorVentas();
        }
    };

    private final TipoContenidoPlan tipoDeContenido;
    private final boolean usaInventario;

    VistaZona(TipoContenidoPlan tipoDeContenido, boolean usaInventario) {
        this.tipoDeContenido = tipoDeContenido;
        this.usaInventario = usaInventario;
    }

    /** El cupo gratuito que gasta esta pantalla. */
    public TipoContenidoPlan tipoDeContenido() {
        return tipoDeContenido;
    }

    /** Si el orden depende de los productos del negocio (hace falta mirar su inventario). */
    public boolean usaInventario() {
        return usaInventario;
    }

    /** La zona con los productos en el orden en que esta pantalla los quiere ver (y desbloquear). */
    public abstract AnalisisZona ordenar(AnalisisZona analisis, List<String> nombresPropios);
}
