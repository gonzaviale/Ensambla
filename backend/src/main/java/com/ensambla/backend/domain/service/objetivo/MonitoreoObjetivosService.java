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
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mide cómo viene cada objetivo de un negocio: el resultado y la métrica de cada mini-objetivo en curso, con una foto
 * por día. Marca como logrado lo que llegó a su meta y como alcanzado el objetivo cuyo resultado la alcanzó, y a un
 * objetivo monetario que nunca tuvo mini-objetivos le propone los suyos. Los mini-objetivos manuales no se miden. Lo usan el monitoreo de cada hora y la medición que se
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
        MedidorMiniObjetivo medidor = medirElNegocio(negocioId, hoy);
        obtenerObjetivos(negocioId).forEach(objetivo -> monitorear(negocioId, objetivo, medidor, hoy, ahora));
    }

    private void monitorear(
            NegocioId negocioId, Objetivo objetivo, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        BigDecimal resultado = medidor.resultado();
        marcarAlcanzadoSiLlego(objetivo, resultado, ahora);
        registrarResultadoDelDia(objetivo, resultado, hoy, ahora);
        List<MiniObjetivo> minis = minisDe(negocioId, objetivo, medidor, hoy, ahora);
        minis.stream()
                .filter(MiniObjetivo::estaEnCurso)
                .filter(MiniObjetivo::esMedido)
                .forEach(mini -> medir(mini, medidor, hoy, ahora));
    }

    private void marcarAlcanzadoSiLlego(Objetivo objetivo, BigDecimal resultado, OffsetDateTime ahora) {
        if (objetivo.estaAlcanzado() || !objetivo.llegaA(resultado)) {
            return;
        }
        objetivo.marcarAlcanzado(ahora);
        guardarObjetivo(objetivo);
    }

    private void registrarResultadoDelDia(
            Objetivo objetivo, BigDecimal resultado, LocalDate hoy, OffsetDateTime ahora) {
        registrarProgreso(ProgresoObjetivo.de(objetivo.getId(), null, hoy, resultado, ahora));
    }

    private List<MiniObjetivo> minisDe(
            NegocioId negocioId, Objetivo objetivo, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        List<MiniObjetivo> minis = obtenerMinis(negocioId, objetivo);
        if (minis.isEmpty() && objetivo.getTipo().tieneMeta() && !objetivo.estaAlcanzado()) {
            return proponer(negocioId, objetivo, medidor, hoy, ahora);
        }
        return minis;
    }

    /** Un objetivo sin mini-objetivos (anterior a esta función, o sin datos cuando se creó) recibe los suyos. */
    private List<MiniObjetivo> proponer(
            NegocioId negocioId, Objetivo objetivo, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        List<CandidatoMiniObjetivo> elegidos = CandidatosMiniObjetivo.elegir(
                obtenerCandidatos(negocioId, medidor), List.of(), MiniObjetivo.MAXIMO_EN_CURSO);
        return MiniObjetivo.propuestos(objetivo, elegidos, SemanaObjetivo.lunesDe(hoy), ahora).stream()
                .map(this::guardarMini)
                .toList();
    }

    /** Un mini-objetivo cuyo producto ya no se puede medir queda como estaba: el cierre semanal lo reemplaza. */
    private void medir(MiniObjetivo mini, MedidorMiniObjetivo medidor, LocalDate hoy, OffsetDateTime ahora) {
        medidor.valorDe(mini.getTipo(), mini.getRegistroId(), mini.getReferencia())
                .ifPresent(valor -> anotarMedicion(mini, valor, hoy, ahora));
    }

    private void anotarMedicion(MiniObjetivo mini, BigDecimal valor, LocalDate hoy, OffsetDateTime ahora) {
        mini.medir(valor, hoy, ahora);
        guardarMini(mini);
        registrarProgreso(ProgresoObjetivo.de(mini.getObjetivoId(), mini.getId(), hoy, mini.getValorActual(), ahora));
    }

    private MedidorMiniObjetivo medirElNegocio(NegocioId negocioId, LocalDate hoy) {
        return propuestaMiniObjetivosService.medidor(negocioId, hoy);
    }

    private List<CandidatoMiniObjetivo> obtenerCandidatos(NegocioId negocioId, MedidorMiniObjetivo medidor) {
        return propuestaMiniObjetivosService.candidatos(negocioId, medidor);
    }

    private List<Objetivo> obtenerObjetivos(NegocioId negocioId) {
        return objetivoRepository.findAllByNegocioId(negocioId);
    }

    private List<MiniObjetivo> obtenerMinis(NegocioId negocioId, Objetivo objetivo) {
        return miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
    }

    private void guardarObjetivo(Objetivo objetivo) {
        objetivoRepository.save(objetivo);
    }

    private MiniObjetivo guardarMini(MiniObjetivo mini) {
        return miniObjetivoRepository.save(mini);
    }

    private void registrarProgreso(ProgresoObjetivo progreso) {
        progresoObjetivoRepository.registrar(progreso);
    }
}
