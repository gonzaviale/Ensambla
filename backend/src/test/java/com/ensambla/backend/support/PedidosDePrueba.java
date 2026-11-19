package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.DiccionarioDePedidos;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.InterpreteDePedidos;
import com.ensambla.backend.domain.model.metrica.ParteDeLaReceta;
import com.ensambla.backend.domain.model.metrica.PedidoInterpretado;
import com.ensambla.backend.infrastructure.config.MetricasConfig;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/** El intérprete de pedidos con el diccionario real de la aplicación, y diccionarios chicos para casos puntuales. */
public final class PedidosDePrueba {

    private PedidosDePrueba() {
    }

    /** El que usa la aplicación: lee {@code metricas/diccionario-pedidos.json}. */
    public static InterpreteDePedidos elInterpreteDeLaAplicacion() {
        return new MetricasConfig().interpreteDePedidos(JsonMapper.builder().build());
    }

    /** «Cuánto vendí el finde»: sólo dijo la tabla, el resto se tomó por defecto y no entendió «finde». */
    public static PedidoInterpretado unPedidoDelFindeInterpretadoComo(DefinicionMetrica receta) {
        return new PedidoInterpretado(receta, List.of(ParteDeLaReceta.FUENTE),
                List.of(ParteDeLaReceta.CALCULO, ParteDeLaReceta.AGRUPACION, ParteDeLaReceta.PERIODO),
                List.of("finde"));
    }

    /** Un diccionario que sólo reconoce las ventas, con dos formas de nombrarlas. */
    public static DiccionarioDePedidos unDiccionarioQueSoloConoceLasVentas() {
        return new DiccionarioDePedidos(Map.of(FuenteMetrica.VENTAS, List.of("vendí", "ventas")), null, null, null,
                null, null, null);
    }
}
