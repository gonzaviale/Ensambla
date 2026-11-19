package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Cuánto pesa cada factor al puntuar una zona: según el rubro, el público y lo que le importe a la persona. */
final class PesosDeFactores {

    /** En el orden de {@link FactorDondeAbrir}. */
    private static final Map<RubroNegocio, List<Double>> BASE_POR_RUBRO = Map.of(
            RubroNegocio.ALMACEN, List.of(.20, .10, .22, .05, .03, .05, .03, .02, .07, .09, .14, .14),
            RubroNegocio.PAPELERIA, List.of(.20, .11, .05, .24, .13, .05, .02, .01, .07, .05, .08, .07),
            RubroNegocio.KIOSCO, List.of(.13, .24, .03, .11, .11, .18, .03, .02, .05, .03, .12, .07));

    private static final Map<PublicoDondeAbrir, Map<FactorDondeAbrir, Double>> MULTIPLICADORES_POR_PUBLICO = Map.of(
            PublicoDondeAbrir.VECINOS, Map.of(FactorDondeAbrir.DENSIDAD, 1.6d),
            PublicoDondeAbrir.ESTUDIANTES, Map.of(FactorDondeAbrir.ESCUELAS, 1.8d, FactorDondeAbrir.PARQUES, 1.2d),
            PublicoDondeAbrir.OFICINAS, Map.of(FactorDondeAbrir.OFICINAS, 1.8d, FactorDondeAbrir.TRANSPORTE, 1.2d));

    /** La importancia que marca la persona (0, 1 o 2) y cuánto multiplica el peso del factor. */
    private static final Map<Integer, Double> MULTIPLICADORES_POR_IMPORTANCIA = Map.of(0, 0d, 1, 1d, 2, 2.2d);

    private PesosDeFactores() {
    }

    /** Los pesos de cada factor para el rubro, antes de ajustarlos. */
    static Map<FactorDondeAbrir, Double> base(RubroNegocio rubro) {
        Map<FactorDondeAbrir, Double> pesos = new EnumMap<>(FactorDondeAbrir.class);
        List<Double> valores = BASE_POR_RUBRO.get(rubro);
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            pesos.put(factor, valores.get(factor.ordinal()));
        }
        return pesos;
    }

    /** Le da más peso a los factores que más le importan a este público. */
    static void ajustarPorPublico(Map<FactorDondeAbrir, Double> pesos, PublicoDondeAbrir publico) {
        MULTIPLICADORES_POR_PUBLICO.get(publico).forEach((factor, multiplicador) ->
                pesos.computeIfPresent(factor, (ignorado, peso) -> peso * multiplicador));
    }

    /** Cuánto multiplica el peso la importancia marcada; si es otro número, lo deja como está. */
    static double multiplicadorDeImportancia(int nivel) {
        return MULTIPLICADORES_POR_IMPORTANCIA.getOrDefault(nivel, 1d);
    }
}
