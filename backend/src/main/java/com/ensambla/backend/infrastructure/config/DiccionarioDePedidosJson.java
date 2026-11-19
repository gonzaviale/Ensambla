package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DiccionarioDePedidos;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;

import java.util.List;
import java.util.Map;

/** El diccionario tal como está escrito en {@code metricas/diccionario-pedidos.json}. */
record DiccionarioDePedidosJson(
        Map<FuenteMetrica, List<String>> fuentes,
        Map<CalculoMetrica, List<String>> calculos,
        Map<AgrupacionMetrica, List<String>> agrupaciones,
        Map<PeriodoMetrica, List<String>> periodos,
        List<String> fiado,
        List<String> noSeCalculan,
        List<String> palabrasVacias) {

    DiccionarioDePedidos aDiccionario() {
        return new DiccionarioDePedidos(fuentes, calculos, agrupaciones, periodos, fiado, noSeCalculan, palabrasVacias);
    }
}
