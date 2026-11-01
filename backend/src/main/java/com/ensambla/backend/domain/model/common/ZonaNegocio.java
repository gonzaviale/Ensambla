package com.ensambla.backend.domain.model.common;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * La zona horaria del negocio: Argentina. Los días y los meses de las cuotas (límites diarios de IA, cupo mensual de
 * análisis de foto, avisos y recomendaciones del plan) empiezan a medianoche de acá, no de UTC. Los instantes se siguen
 * guardando y comparando en UTC; sólo el corte de calendario usa esta zona.
 */
public final class ZonaNegocio {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private ZonaNegocio() {
    }

    /** El día de calendario argentino en el que cae el instante. */
    public static LocalDate dia(OffsetDateTime instante) {
        return instante.atZoneSameInstant(ZONA).toLocalDate();
    }

    /** El primer día del mes argentino en el que cae el instante. */
    public static LocalDate primerDiaDelMes(OffsetDateTime instante) {
        return dia(instante).withDayOfMonth(1);
    }

    /** El instante en que empieza ese día en Argentina (su medianoche), expresado en UTC como el resto de los instantes. */
    public static OffsetDateTime inicioDelDia(LocalDate dia) {
        return dia.atStartOfDay(ZONA).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
