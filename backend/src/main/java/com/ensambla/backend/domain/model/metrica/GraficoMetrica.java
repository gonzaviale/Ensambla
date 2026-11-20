package com.ensambla.backend.domain.model.metrica;

import java.util.Arrays;
import java.util.List;

/**
 * Cómo se dibuja una métrica en su solapa. El número grande sirve siempre; las barras y la dona necesitan un reparto
 * (por producto, cliente, categoría o día) y la línea, algo que va en orden (los días de la semana).
 */
public enum GraficoMetrica {

    NUMERO {
        @Override
        boolean sirvePara(AgrupacionMetrica agrupacion) {
            return true;
        }
    },
    BARRAS {
        @Override
        boolean sirvePara(AgrupacionMetrica agrupacion) {
            return agrupacion.reparte();
        }
    },
    DONA {
        @Override
        boolean sirvePara(AgrupacionMetrica agrupacion) {
            return agrupacion.reparte();
        }
    },
    LINEA {
        @Override
        boolean sirvePara(AgrupacionMetrica agrupacion) {
            return agrupacion.vaEnOrden();
        }
    };

    abstract boolean sirvePara(AgrupacionMetrica agrupacion);

    /** Los gráficos que tienen sentido para una métrica repartida así, en el orden en que se ofrecen. */
    public static List<GraficoMetrica> posiblesPara(AgrupacionMetrica agrupacion) {
        return Arrays.stream(values()).filter(grafico -> grafico.sirvePara(agrupacion)).toList();
    }
}
