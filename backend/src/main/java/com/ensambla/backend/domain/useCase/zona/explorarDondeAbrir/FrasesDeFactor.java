package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;

import java.util.Map;
import java.util.function.Function;

import static java.util.Map.entry;

/** Lo que Dónde abrir le cuenta a la persona de cada factor: por qué juega a favor de una zona y por qué es un riesgo. */
final class FrasesDeFactor {

    /** Lo que se midió de un factor, listo para decirlo en una frase: la cantidad, su unidad y el comercio del rubro. */
    record Medida(String cantidad, String unidad, String comercios) {
    }

    private record Frases(Function<Medida, String> aFavor, Function<Medida, String> enContra) {
    }

    private static final Map<FactorDondeAbrir, Frases> POR_FACTOR = Map.ofEntries(
            entry(FactorDondeAbrir.COMPETENCIA, new Frases(
                    medida -> "Solo " + medida.cantidad() + " " + medida.comercios() + " a menos de 500 m",
                    medida -> "Hay " + medida.cantidad() + " " + medida.comercios()
                            + " a menos de 500 m: la competencia es fuerte")),
            entry(FactorDondeAbrir.ACTIVIDAD_ACCESO, new Frases(
                    medida -> "Buena actividad y acceso estimados (" + medida.cantidad() + " de 100)",
                    medida -> "La actividad y el acceso estimados son bajos")),
            entry(FactorDondeAbrir.DENSIDAD, new Frases(
                    medida -> "Zona con muchos vecinos (" + medida.cantidad() + " personas por km²)",
                    medida -> "Hay pocos vecinos viviendo cerca")),
            entry(FactorDondeAbrir.ESCUELAS, new Frases(
                    medida -> medida.cantidad() + " escuelas a menos de 500 m",
                    medida -> "Casi no hay escuelas cerca")),
            entry(FactorDondeAbrir.OFICINAS, new Frases(
                    medida -> medida.cantidad() + " oficinas a menos de 500 m",
                    medida -> "Casi no hay oficinas cerca")),
            entry(FactorDondeAbrir.TRANSPORTE, new Frases(
                    medida -> medida.cantidad() + " paradas y estaciones a menos de 500 m",
                    medida -> "Hay pocas paradas y estaciones cerca")),
            entry(FactorDondeAbrir.PARQUES, new Frases(
                    medida -> medida.cantidad() + " parques y plazas a menos de 500 m",
                    medida -> "Casi no hay parques ni plazas cerca")),
            entry(FactorDondeAbrir.SALUD, new Frases(
                    medida -> medida.cantidad() + " centros de salud cerca",
                    medida -> "Hay pocos centros de salud cerca")),
            entry(FactorDondeAbrir.COMPLEMENTARIOS, new Frases(
                    medida -> medida.cantidad() + " comercios que atraen gente",
                    medida -> "Hay pocos comercios que atraigan gente")),
            entry(FactorDondeAbrir.PERFIL_SOCIOECONOMICO, new Frases(
                    medida -> "Buen perfil de poder de compra",
                    medida -> "El poder de compra estimado es más bajo que en otras zonas")),
            entry(FactorDondeAbrir.DEMANDA_ENSAMBLA, new Frases(
                    medida -> "Demanda observada en comercios de Ensambla (" + medida.cantidad() + " "
                            + medida.unidad() + ")",
                    medida -> "Ensambla observa menos demanda que en otras zonas con datos suficientes")),
            entry(FactorDondeAbrir.ALQUILER, new Frases(
                    medida -> "Cotización de alquiler competitiva entre las zonas informadas",
                    medida -> "La cotización de alquiler es alta frente a las otras que cargaste")));

    private FrasesDeFactor() {
    }

    /** Por qué este factor juega a favor de la zona. */
    static String aFavor(FactorDondeAbrir factor, Medida medida) {
        return POR_FACTOR.get(factor).aFavor().apply(medida);
    }

    /** Por qué este factor es un riesgo para la zona. */
    static String enContra(FactorDondeAbrir factor, Medida medida) {
        return POR_FACTOR.get(factor).enContra().apply(medida);
    }
}
