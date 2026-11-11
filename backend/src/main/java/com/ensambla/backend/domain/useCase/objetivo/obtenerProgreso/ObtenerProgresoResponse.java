package com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso;

import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cómo viene un objetivo y cada uno de sus mini-objetivos en curso o logrados, semana por semana.
 *
 * @param meta    el resultado mensual que busca el objetivo
 * @param hito    {@code OBJETIVO_ALCANZADO} si ya llegó a la meta; vacío si no
 * @param semanas el resultado del negocio al final de cada semana, de la más vieja a la más nueva
 * @param minis   los mini-objetivos en curso y logrados, con su propia serie
 */
public record ObtenerProgresoResponse(
        BigDecimal meta,
        HitoObjetivo hito,
        List<PuntoSemanal> semanas,
        List<MiniProgreso> minis) {

    /**
     * El último valor de la semana que empieza el {@code semana}; {@code estimado} si no lo midió el monitoreo sino
     * que se calculó después con los cuadernos.
     */
    public record PuntoSemanal(LocalDate semana, BigDecimal valor, boolean estimado) {

        public static PuntoSemanal de(ProgresoObjetivo progreso) {
            return new PuntoSemanal(SemanaObjetivo.lunesDe(progreso.dia()), progreso.valor(), false);
        }
    }

    public record MiniProgreso(MiniObjetivoItem mini, HitoObjetivo hito, List<PuntoSemanal> semanas) {

        public static MiniProgreso de(MiniObjetivo mini, LocalDate hoy, List<ProgresoObjetivo> fotos) {
            return new MiniProgreso(
                    MiniObjetivoItem.de(mini),
                    mini.hito(hoy).orElse(null),
                    ProgresoObjetivo.ultimaDeCadaSemana(fotos).stream().map(PuntoSemanal::de).toList());
        }
    }

    public static ObtenerProgresoResponse de(Objetivo objetivo, List<PuntoSemanal> semanas, List<MiniProgreso> minis) {
        return new ObtenerProgresoResponse(objetivo.getMeta(), objetivo.hito().orElse(null), semanas, minis);
    }
}
