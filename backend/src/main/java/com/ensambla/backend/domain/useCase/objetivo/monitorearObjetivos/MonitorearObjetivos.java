package com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ProgresoObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mide en silencio cómo viene cada objetivo: el resultado del negocio y la métrica de cada mini-objetivo en curso, y
 * guarda una foto por día. Marca como logrado lo que llegó a su meta y como alcanzado el objetivo cuyo resultado la
 * alcanzó, y a un objetivo que nunca tuvo mini-objetivos le propone los suyos (con texto de reserva, sin IA).
 * <p>
 * No recibe ningún puerto que avise o mande mensajes: lo que deja sólo se ve cuando la persona entra a la app. Cada
 * negocio va en su propia transacción y uno con error no frena a los demás.
 */
@Slf4j
@RequiredArgsConstructor
public class MonitorearObjetivos {

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final ProgresoObjetivoRepository progresoObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Cuántos negocios se piden por vez, para no cargar a todos juntos. */
    private final int tamanoTanda;

    public void execute() {
        int pagina = 0;
        List<NegocioId> tanda;
        do {
            tanda = objetivoRepository.findNegocioIdsConObjetivos(pagina++, tamanoTanda);
            tanda.forEach(this::monitorearNegocio);
        } while (tanda.size() == tamanoTanda);
    }

    private void monitorearNegocio(NegocioId negocioId) {
        try {
            OffsetDateTime ahora = OffsetDateTime.now(clock);
            transaccionPort.ejecutar(() -> monitorear(negocioId, ahora));
        } catch (RuntimeException e) {
            log.warn("No se pudo monitorear los objetivos del negocio {}", negocioId, e);
        }
    }

    private void monitorear(NegocioId negocioId, OffsetDateTime ahora) {
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
