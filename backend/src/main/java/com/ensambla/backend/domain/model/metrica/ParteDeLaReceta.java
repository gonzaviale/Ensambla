package com.ensambla.backend.domain.model.metrica;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** Cada parte de la receta que arma un pedido en palabras: si la persona la dijo o se tomó la de siempre. */
public enum ParteDeLaReceta {

    FUENTE {
        @Override
        boolean laDijo(LoQueDiceElPedido pedido) {
            return true;
        }
    },
    CALCULO {
        @Override
        boolean laDijo(LoQueDiceElPedido pedido) {
            return pedido.calculo().isPresent();
        }
    },
    AGRUPACION {
        @Override
        boolean laDijo(LoQueDiceElPedido pedido) {
            return pedido.agrupacion().isPresent();
        }
    },
    /** El stock no tiene período: es lo que hay hoy, así que no se toma ninguno por defecto. */
    PERIODO {
        @Override
        boolean laDijo(LoQueDiceElPedido pedido) {
            return pedido.periodo().isPresent();
        }

        @Override
        boolean seTomoLaDeSiempre(LoQueDiceElPedido pedido) {
            return !laDijo(pedido) && pedido.fuente().tieneFecha();
        }
    },
    /** Mirar sólo lo fiado se pide o no se pide: si no se dice, no hay nada que avisar. */
    SOLO_FIADO {
        @Override
        boolean laDijo(LoQueDiceElPedido pedido) {
            return pedido.soloFiado();
        }

        @Override
        boolean seTomoLaDeSiempre(LoQueDiceElPedido pedido) {
            return false;
        }
    };

    abstract boolean laDijo(LoQueDiceElPedido pedido);

    boolean seTomoLaDeSiempre(LoQueDiceElPedido pedido) {
        return !laDijo(pedido);
    }

    static List<ParteDeLaReceta> lasQueDijo(LoQueDiceElPedido pedido) {
        return lasQue(parte -> parte.laDijo(pedido));
    }

    static List<ParteDeLaReceta> lasDeSiempre(LoQueDiceElPedido pedido) {
        return lasQue(parte -> parte.seTomoLaDeSiempre(pedido));
    }

    private static List<ParteDeLaReceta> lasQue(Predicate<ParteDeLaReceta> cumplen) {
        return Arrays.stream(values()).filter(cumplen).toList();
    }
}
