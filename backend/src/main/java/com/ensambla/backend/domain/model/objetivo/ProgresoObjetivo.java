package com.ensambla.backend.domain.model.objetivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * Una foto del avance, una por día: el resultado del negocio si es del objetivo ({@code miniObjetivoId} vacío) o la
 * métrica de un mini-objetivo. La última medición del día pisa a la anterior.
 */
public record ProgresoObjetivo(
        ProgresoObjetivoId id,
        ObjetivoId objetivoId,
        MiniObjetivoId miniObjetivoId,
        LocalDate dia,
        BigDecimal valor,
        OffsetDateTime medidoEn) {

    /** Una foto nueva; {@code miniObjetivoId} es {@code null} para la del objetivo mayor. */
    public static ProgresoObjetivo de(
            ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId, LocalDate dia, BigDecimal valor,
            OffsetDateTime medidoEn) {
        return new ProgresoObjetivo(ProgresoObjetivoId.nuevo(), objetivoId, miniObjetivoId, dia, valor, medidoEn);
    }

    /** Si es la foto del objetivo mayor y no la de un mini-objetivo. */
    public boolean esDelObjetivo() {
        return miniObjetivoId == null;
    }

    /** La última foto de cada semana (de lunes a domingo), de la semana más vieja a la más nueva. */
    public static List<ProgresoObjetivo> ultimaDeCadaSemana(List<ProgresoObjetivo> fotos) {
        return fotos.stream()
                .collect(Collectors.toMap(
                        foto -> SemanaObjetivo.lunesDe(foto.dia()),
                        foto -> foto,
                        BinaryOperator.maxBy(comparing(ProgresoObjetivo::dia)),
                        TreeMap::new))
                .values().stream()
                .toList();
    }
}
