package com.ensambla.backend.domain.service.objetivo;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mide cómo viene cada objetivo de un negocio: el resultado y la métrica de cada mini-objetivo en curso, con una foto
 * por día. Marca como logrado lo que llegó a su meta y como alcanzado el objetivo cuyo resultado la alcanzó, y a un
 * objetivo que nunca tuvo mini-objetivos le propone los suyos. Lo usan el monitoreo de cada hora y la medición que se
 * pide al abrir los objetivos, para que una venta recién cargada se vea enseguida.
 */
@RequiredArgsConstructor
public class MonitoreoObjetivosService {

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final ProgresoObjetivoRepository progresoObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    /** Mide los objetivos del negocio con lo que tienen hoy sus cuadernos; se llama dentro de una transacción. */
    public void monitorear(NegocioId negocioId, OffsetDateTime ahora) {
        LocalDate hoy = ZonaNegocio.dia(ahora);
        MedidorMiniObjetivo medidor = propuestaMiniObjetivosService.medidor(negocioId, hoy);
        objetivoRepository.findAllByNegocioId(negocioId)
                .forEach(objetivo -> monitorear(negocioId, objetivo, medidor, hoy, ahora));
    }

    private void monitorear(
            NegocioId negocioId, Objetivo objetivo, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        BigDecimal resultado = medidor.resultado();
        if (!objetivo.estaAlcanzado() && objetivo.llegaA(resultado)) {
            objetivo.marcarAlcanzado(ahora);
            objetivoRepository.save(objetivo);
        }
        progresoObjetivoRepository.registrar(ProgresoObjetivo.de(objetivo.getId(), null, hoy, resultado, ahora));
        List<MiniObjetivo> minis = miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
        if (minis.isEmpty() && !objetivo.estaAlcanzado()) {
            minis = proponer(negocioId, objetivo, medidor, hoy, ahora);
        }
        minis.stream()
                .filter(MiniObjetivo::estaEnCurso)
                .forEach(mini -> medir(mini, medidor, hoy, ahora));
    }

    /** Un objetivo sin mini-objetivos (anterior a esta función, o sin datos cuando se creó) recibe los suyos. */
    private List<MiniObjetivo> proponer(
            NegocioId negocioId, Objetivo objetivo, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        return MiniObjetivo.propuestos(
                        objetivo,
                        CandidatosMiniObjetivo.elegir(
                                propuestaMiniObjetivosService.candidatos(negocioId, medidor),
                                List.of(),
                                MiniObjetivo.MAXIMO_EN_CURSO),
                        SemanaObjetivo.lunesDe(hoy),
                        ahora).stream()
                .map(miniObjetivoRepository::save)
                .toList();
    }

    /** Un mini-objetivo cuyo producto ya no se puede medir queda como estaba: el cierre semanal lo reemplaza. */
    private void medir(MiniObjetivo mini, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        medidor.valorDe(mini.getTipo(), mini.getRegistroId(), mini.getReferencia()).ifPresent(valor -> {
            mini.medir(valor, hoy, ahora);
            miniObjetivoRepository.save(mini);
            progresoObjetivoRepository.registrar(
                    ProgresoObjetivo.de(mini.getObjetivoId(), mini.getId(), hoy, mini.getValorActual(), ahora));
        });
    }
}
