package com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MotivoReajuste;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReglasReajuste;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TextoResumen;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El cierre del domingo: por cada objetivo, cierra las semanas que ya terminaron y todavía no tienen resumen (hasta
 * {@code maxSemanasPendientes}, las más nuevas, por si el servidor estuvo caído). Cada semana se recalcula con los
 * cuadernos a ese domingo, se aplican las {@link ReglasReajuste} a los mini-objetivos y se guarda el resumen con sus
 * reajustes en una sola transacción. Si otra corrida ya cerró esa semana, se deshace y se sigue con la próxima.
 * <p>
 * No crea avisos ni manda mensajes: el resumen sólo se ve cuando la persona entra a la app. Lee los avisos sólo para
 * saber si una decisión movió las ventas. Cada negocio va aparte y uno con error no frena a los demás.
 */
@Slf4j
@RequiredArgsConstructor
public class CerrarSemanasObjetivos {

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final DecisionObjetivoRepository decisionObjetivoRepository;

    private final AvisoRepository avisoRepository;

    private final ResumenSemanalRepository resumenSemanalRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Cuántas semanas sin cerrar se recuperan como mucho por objetivo; las más viejas se omiten. */
    private final int maxSemanasPendientes;

    /** Cuántos negocios se piden por vez, para no cargar a todos juntos. */
    private final int tamanoTanda;

    public void execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LocalDate ultima = SemanaObjetivo.ultimaCerrable(ahora);
        int pagina = 0;
        List<NegocioId> tanda;
        do {
            tanda = objetivoRepository.findNegocioIdsConObjetivos(pagina++, tamanoTanda);
            tanda.forEach(negocioId -> cerrarNegocio(negocioId, ultima, ahora));
        } while (tanda.size() == tamanoTanda);
    }

    private void cerrarNegocio(NegocioId negocioId, LocalDate ultima, OffsetDateTime ahora) {
        try {
            Map<LocalDate, MedidorMiniObjetivo> medidores = new HashMap<>();
            objetivoRepository.findAllByNegocioId(negocioId)
                    .forEach(objetivo -> cerrarObjetivo(negocioId, objetivo, ultima, medidores, ahora));
        } catch (RuntimeException e) {
            log.warn("No se pudo cerrar la semana de los objetivos del negocio {}", negocioId, e);
        }
    }

    /** Los objetivos del negocio comparten lo medido al domingo de cada semana: se carga una sola vez. */
    private void cerrarObjetivo(
            NegocioId negocioId, Objetivo objetivo, LocalDate ultima, Map<LocalDate, MedidorMiniObjetivo> medidores,
            OffsetDateTime ahora) {
        List<ResumenSemanal> resumenes =
                new ArrayList<>(resumenSemanalRepository.findAllByObjetivoId(objetivo.getId()));
        for (LocalDate semana : pendientes(objetivo, resumenes, ultima)) {
            MedidorMiniObjetivo medidor = medidores.computeIfAbsent(semana,
                    lunes -> propuestaMiniObjetivosService.medidor(negocioId, SemanaObjetivo.domingo(lunes)));
            try {
                resumenes.add(0, transaccionPort.ejecutar(
                        () -> cerrar(negocioId, objetivo, semana, medidor, resumenes, ahora)));
            } catch (DuplicateResourceException e) {
                log.info("La semana {} del objetivo {} ya estaba cerrada", semana, objetivo.getId());
            }
        }
    }

    /**
     * Las semanas por cerrar, de la más vieja a la más nueva: desde la de creación del objetivo o la siguiente a la
     * última cerrada, hasta {@code ultima}, y como mucho las {@code maxSemanasPendientes} más nuevas.
     */
    private List<LocalDate> pendientes(Objetivo objetivo, List<ResumenSemanal> resumenes, LocalDate ultima) {
        LocalDate desde = SemanaObjetivo.lunesDe(ZonaNegocio.dia(objetivo.getCreadoEn()));
        if (!resumenes.isEmpty() && resumenes.get(0).semanaInicio().plusWeeks(1).isAfter(desde)) {
            desde = resumenes.get(0).semanaInicio().plusWeeks(1);
        }
        LocalDate tope = ultima.minusWeeks(maxSemanasPendientes - 1L);
        if (desde.isBefore(tope)) {
            desde = tope;
        }
        List<LocalDate> semanas = new ArrayList<>();
        for (LocalDate semana = desde; !semana.isAfter(ultima); semana = semana.plusWeeks(1)) {
            semanas.add(semana);
        }
        return semanas;
    }

    private ResumenSemanal cerrar(
            NegocioId negocioId, Objetivo objetivo, LocalDate semana, MedidorMiniObjetivo medidor,
            List<ResumenSemanal> anteriores, OffsetDateTime ahora) {
        List<MiniObjetivo> minis = miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
        List<DecisionObjetivo> decisiones =
                decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
        BigDecimal resultado = medidor.resultado();
        boolean alcanzado = objetivo.estaAlcanzado() || objetivo.llegaA(resultado);
        boolean sinVentas = medidor.ventasSemana().signum() == 0;
        List<CierreMini> cierres = cierres(minis, medidor);
        List<ReajusteMiniObjetivo> reajustes = alcanzado || sinVentas
                ? List.of()
                : reajustar(negocioId, objetivo, semana, medidor, minis, decisiones, anteriores, ahora);
        ConteoDecisiones conteo = ConteoDecisiones.de(decisiones.stream()
                .filter(decision -> decision.semanaInicio().equals(semana))
                .toList());
        String queFunciono = TextoResumen.queFunciono(
                resultado, resultadoDe(anteriores, semana.minusWeeks(1)), conteo, objetivo.getMeta(), alcanzado,
                sinVentas);
        return resumenSemanalRepository.save(
                ResumenSemanal.de(objetivo, semana, resultado, conteo, cierres, queFunciono, reajustes, ahora));
    }

    /** El valor al domingo de los mini-objetivos en curso o logrados, para compararlo en los cierres siguientes. */
    private static List<CierreMini> cierres(List<MiniObjetivo> minis, MedidorMiniObjetivo medidor) {
        return minis.stream()
                .filter(mini -> mini.estaEnCurso() || mini.esperaReajuste())
                .flatMap(mini -> medidor.valorDe(mini.getTipo(), mini.getRegistroId(), mini.getReferencia())
                        .map(valor -> new CierreMini(mini.getId(), valor))
                        .stream())
                .toList();
    }

    private List<ReajusteMiniObjetivo> reajustar(
            NegocioId negocioId, Objetivo objetivo, LocalDate semana, MedidorMiniObjetivo medidor,
            List<MiniObjetivo> minis, List<DecisionObjetivo> decisiones, List<ResumenSemanal> anteriores,
            OffsetDateTime ahora) {
        Reajuste reajuste = new Reajuste(
                objetivo,
                semana,
                propuestaMiniObjetivosService.candidatos(negocioId, medidor),
                new ArrayList<>(minis.stream().filter(MiniObjetivo::estaEnCurso).toList()),
                new ArrayList<>(ReglasReajuste.usados(minis, semana)),
                ahora);
        Map<AvisoId, Aviso> avisos = avisosDe(decisiones, negocioId);
        Optional<ResumenSemanal> dosAtras = anteriores.stream()
                .filter(resumen -> resumen.semanaInicio().equals(semana.minusWeeks(2)))
                .findFirst();
        List<ReajusteMiniObjetivo> reajustes = new ArrayList<>();
        for (MiniObjetivo mini : minis) {
            Optional<BigDecimal> valor =
                    medidor.valorDe(mini.getTipo(), mini.getRegistroId(), mini.getReferencia());
            ReglasReajuste.decidir(
                            mini,
                            valor,
                            dosAtras.flatMap(resumen -> resumen.valorDe(mini.getId())),
                            ReglasReajuste.decisionSinEfecto(mini, decisiones, avisos),
                            semana)
                    .flatMap(motivo -> reajuste.aplicar(mini, motivo, valor.orElse(mini.getValorActual())))
                    .ifPresent(reajustes::add);
        }
        return reajustes;
    }

    private Map<AvisoId, Aviso> avisosDe(List<DecisionObjetivo> decisiones, NegocioId negocioId) {
        List<AvisoId> ids = decisiones.stream().map(DecisionObjetivo::avisoId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return avisoRepository.findAllByIdsAndNegocioId(ids, negocioId).stream()
                .collect(Collectors.toMap(Aviso::getId, Function.identity()));
    }

    private static Optional<BigDecimal> resultadoDe(List<ResumenSemanal> resumenes, LocalDate semana) {
        return resumenes.stream()
                .filter(resumen -> resumen.semanaInicio().equals(semana))
                .map(ResumenSemanal::resultado)
                .findFirst();
    }

    /**
     * Los reajustes de un cierre: lleva qué mini-objetivos quedan en curso y cuáles ya se usaron, para que dos lugares
     * no reciban el mismo candidato. Los nuevos empiezan la semana siguiente.
     */
    private final class Reajuste {

        private final Objetivo objetivo;

        private final LocalDate semanaSiguiente;

        private final List<CandidatoMiniObjetivo> candidatos;

        private final List<MiniObjetivo> enCurso;

        private final List<MiniObjetivo> usados;

        private final OffsetDateTime ahora;

        private Reajuste(
                Objetivo objetivo, LocalDate semana, List<CandidatoMiniObjetivo> candidatos,
                List<MiniObjetivo> enCurso, List<MiniObjetivo> usados, OffsetDateTime ahora) {
            this.objetivo = objetivo;
            this.semanaSiguiente = semana.plusWeeks(1);
            this.candidatos = candidatos;
            this.enCurso = enCurso;
            this.usados = usados;
            this.ahora = ahora;
        }

        /**
         * Un logrado sigue con una meta más alta; si ya no puede pedir más, o por cualquier otro motivo, se cambia por
         * el mejor candidato. Sin candidato se mantiene. Un lugar que ya tiene a otro en curso no se toca.
         */
        Optional<ReajusteMiniObjetivo> aplicar(MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre) {
            if (!mini.estaEnCurso() && lugarOcupado(mini)) {
                return Optional.empty();
            }
            if (motivo == MotivoReajuste.LOGRADO) {
                Optional<BigDecimal> meta = mini.metaSiguiente(valorAlCierre);
                if (meta.isPresent()) {
                    return Optional.of(subirMeta(mini, valorAlCierre, meta.get()));
                }
            }
            return ReglasReajuste.reemplazo(candidatos, mini, motivo, enCurso, usados)
                    .map(candidato -> reemplazar(mini, motivo, valorAlCierre, candidato));
        }

        private ReajusteMiniObjetivo subirMeta(MiniObjetivo mini, BigDecimal valorAlCierre, BigDecimal meta) {
            String motivo = TextoResumen.metaSubida(mini.getTipo(), valorAlCierre, meta);
            MiniObjetivo nuevo = MiniObjetivo.nuevo(
                    objetivo, mini.getPosicion(), mini.conMeta(valorAlCierre, meta, motivo), semanaSiguiente, ahora);
            mini.lograrYSeguirCon(nuevo.getId(), ahora);
            return guardar(mini, nuevo, TipoReajuste.SUBIR_META, motivo);
        }

        private ReajusteMiniObjetivo reemplazar(
                MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre, CandidatoMiniObjetivo candidato) {
            MiniObjetivo nuevo = MiniObjetivo.nuevo(objetivo, mini.getPosicion(), candidato, semanaSiguiente, ahora);
            if (motivo == MotivoReajuste.LOGRADO) {
                mini.lograrYSeguirCon(nuevo.getId(), ahora);
            } else {
                mini.cederLugarA(nuevo.getId(), ahora);
            }
            TipoReajuste tipo = motivo == MotivoReajuste.LOGRADO ? TipoReajuste.REEMPLAZAR : motivo.tipo();
            return guardar(mini, nuevo, tipo, TextoResumen.motivo(motivo, mini, valorAlCierre, nuevo));
        }

        /** Primero se libera el lugar del anterior y recién después entra el nuevo: un lugar lo ocupa uno solo. */
        private ReajusteMiniObjetivo guardar(
                MiniObjetivo anterior, MiniObjetivo nuevo, TipoReajuste tipo, String motivo) {
            miniObjetivoRepository.save(anterior);
            MiniObjetivo guardado = miniObjetivoRepository.save(nuevo);
            enCurso.removeIf(otro -> otro.getId().equals(anterior.getId()));
            enCurso.add(guardado);
            usados.add(guardado);
            return ReajusteMiniObjetivo.de(anterior, guardado, tipo, motivo, ahora);
        }

        private boolean lugarOcupado(MiniObjetivo mini) {
            return enCurso.stream().anyMatch(otro -> otro.getPosicion() == mini.getPosicion());
        }
    }
}
