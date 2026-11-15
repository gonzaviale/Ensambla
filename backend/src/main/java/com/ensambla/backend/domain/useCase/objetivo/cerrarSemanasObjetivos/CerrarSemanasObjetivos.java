package com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.AccionManual;
import com.ensambla.backend.domain.model.objetivo.AccionRenovacion;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.ManualCierre;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.MotivoReajuste;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReglasReajuste;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TextoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TextoResumen;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.port.RenovacionManualesIaPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
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
 * Los mini-objetivos manuales se renuevan según lo que respondió la persona ({@link AccionRenovacion}), con la IA
 * leyendo lo que contó (sin datos personales) o, sin IA, con un texto de reserva; uno sin respuesta no se renueva y el
 * resumen lo dice. El resumen guarda lo respondido.
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

    private final RespuestaMiniObjetivoRepository respuestaMiniObjetivoRepository;

    private final RenovacionManualesIaPort renovacionManualesIaPort;

    private final ControlPlanService controlPlanService;

    private final ConsumoSamiRepository consumoSamiRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Cuántas semanas sin cerrar se recuperan como mucho por objetivo; las más viejas se omiten. */
    private final int maxSemanasPendientes;

    /** Cuántos negocios se piden por vez, para no cargar a todos juntos. */
    private final int tamanoTanda;

    /** Si la IA lee lo que la persona contó para redactar la acción que renueva cada manual. */
    private final boolean iaHabilitada;

    private final int limiteDiarioIa;

    /** Un manual en curso con la respuesta que la persona le dio. */
    private record Renovable(MiniObjetivo mini, RespuestaMiniObjetivo respuesta) {
    }

    /** Todo lo que hace falta para cerrar una semana de un objetivo. */
    private record Cierre(
            NegocioId negocioId, Objetivo objetivo, LocalDate semana, MedidorMiniObjetivo medidor,
            List<ResumenSemanal> anteriores, Map<MiniObjetivoId, TextoMiniObjetivo> textos, OffsetDateTime ahora) {

        boolean alcanzado() {
            return objetivo.estaAlcanzado() || objetivo.llegaA(medidor.resultado());
        }

        boolean sinVentas() {
            return medidor.ventasSemana().signum() == 0;
        }

        LocalDate semanaSiguiente() {
            return semana.plusWeeks(1);
        }
    }

    public void execute() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LocalDate ultima = SemanaObjetivo.ultimaCerrable(ahora);
        int pagina = 0;
        List<NegocioId> tanda;
        do {
            tanda = obtenerTanda(pagina++);
            tanda.forEach(negocioId -> cerrarNegocio(negocioId, ultima, ahora));
        } while (tanda.size() == tamanoTanda);
    }

    private void cerrarNegocio(NegocioId negocioId, LocalDate ultima, OffsetDateTime ahora) {
        try {
            Map<LocalDate, MedidorMiniObjetivo> medidores = new HashMap<>();
            obtenerObjetivos(negocioId)
                    .forEach(objetivo -> cerrarObjetivo(negocioId, objetivo, ultima, medidores, ahora));
        } catch (RuntimeException e) {
            log.warn("No se pudo cerrar la semana de los objetivos del negocio {}", negocioId, e);
        }
    }

    private void cerrarObjetivo(
            NegocioId negocioId, Objetivo objetivo, LocalDate ultima, Map<LocalDate, MedidorMiniObjetivo> medidores,
            OffsetDateTime ahora) {
        List<ResumenSemanal> resumenes = new ArrayList<>(obtenerResumenes(objetivo));
        for (LocalDate semana : pendientes(objetivo, resumenes, ultima)) {
            MedidorMiniObjetivo medidor = medidorDelDomingo(negocioId, semana, medidores);
            cerrarSemana(negocioId, objetivo, semana, medidor, resumenes, ahora);
        }
    }

    /** Los objetivos del negocio comparten lo medido al domingo de cada semana: se carga una sola vez. */
    private MedidorMiniObjetivo medidorDelDomingo(
            NegocioId negocioId, LocalDate semana, Map<LocalDate, MedidorMiniObjetivo> medidores) {
        return medidores.computeIfAbsent(semana, lunes -> medirElNegocio(negocioId, SemanaObjetivo.domingo(lunes)));
    }

    private void cerrarSemana(
            NegocioId negocioId, Objetivo objetivo, LocalDate semana, MedidorMiniObjetivo medidor,
            List<ResumenSemanal> resumenes, OffsetDateTime ahora) {
        Map<MiniObjetivoId, TextoMiniObjetivo> textos = textosDeRenovacion(negocioId, objetivo, semana, ahora);
        Cierre cierre = new Cierre(negocioId, objetivo, semana, medidor, resumenes, textos, ahora);
        try {
            resumenes.add(0, cerrarEnUnaTransaccion(cierre));
        } catch (DuplicateResourceException e) {
            log.info("La semana {} del objetivo {} ya estaba cerrada", semana, objetivo.getId());
        }
    }

    /**
     * Las semanas por cerrar, de la más vieja a la más nueva: desde la de creación del objetivo o la siguiente a la
     * última cerrada, hasta {@code ultima}, y como mucho las {@code maxSemanasPendientes} más nuevas.
     */
    private List<LocalDate> pendientes(Objetivo objetivo, List<ResumenSemanal> resumenes, LocalDate ultima) {
        LocalDate desde = primeraSemanaSinCerrar(objetivo, resumenes);
        LocalDate tope = ultima.minusWeeks(maxSemanasPendientes - 1L);
        return semanasEntre(desde.isBefore(tope) ? tope : desde, ultima);
    }

    private static LocalDate primeraSemanaSinCerrar(Objetivo objetivo, List<ResumenSemanal> resumenes) {
        LocalDate creacion = SemanaObjetivo.lunesDe(ZonaNegocio.dia(objetivo.getCreadoEn()));
        if (resumenes.isEmpty()) {
            return creacion;
        }
        LocalDate siguienteALaUltimaCerrada = resumenes.get(0).semanaInicio().plusWeeks(1);
        return siguienteALaUltimaCerrada.isAfter(creacion) ? siguienteALaUltimaCerrada : creacion;
    }

    private static List<LocalDate> semanasEntre(LocalDate desde, LocalDate hasta) {
        List<LocalDate> semanas = new ArrayList<>();
        for (LocalDate semana = desde; !semana.isAfter(hasta); semana = semana.plusWeeks(1)) {
            semanas.add(semana);
        }
        return semanas;
    }

    private ResumenSemanal cerrarEnUnaTransaccion(Cierre cierre) {
        return transaccionPort.ejecutar(() -> cerrar(cierre));
    }

    private ResumenSemanal cerrar(Cierre cierre) {
        List<MiniObjetivo> minis = obtenerMinis(cierre);
        List<DecisionObjetivo> decisiones = obtenerDecisiones(cierre);
        List<RespuestaMiniObjetivo> respuestas = obtenerRespuestas(cierre);
        List<CierreMini> valoresAlDomingo = cierres(minis, cierre.medidor());
        List<MiniObjetivo> manuales = manualesEnCurso(minis, cierre.semana());
        List<ManualCierre> cierreManuales = cierresDeManuales(manuales, respuestas);
        List<ReajusteMiniObjetivo> reajustes =
                reajustesDeLaSemana(cierre, minis, decisiones, renovables(manuales, respuestas));
        return guardarResumen(cierre, decisiones, valoresAlDomingo, reajustes, cierreManuales);
    }

    private List<ReajusteMiniObjetivo> reajustesDeLaSemana(
            Cierre cierre, List<MiniObjetivo> minis, List<DecisionObjetivo> decisiones, List<Renovable> renovables) {
        List<ReajusteMiniObjetivo> reajustes = new ArrayList<>(reajustesDeLosMedidos(cierre, minis, decisiones));
        if (!cierre.alcanzado()) {
            reajustes.addAll(renovar(cierre, renovables));
        }
        return reajustes;
    }

    private List<ReajusteMiniObjetivo> reajustesDeLosMedidos(
            Cierre cierre, List<MiniObjetivo> minis, List<DecisionObjetivo> decisiones) {
        if (cierre.alcanzado() || cierre.sinVentas()) {
            return List.of();
        }
        return new Reajuste(cierre, minis, decisiones).reajustarMedidos();
    }

    private ResumenSemanal guardarResumen(
            Cierre cierre, List<DecisionObjetivo> decisiones, List<CierreMini> valoresAlDomingo,
            List<ReajusteMiniObjetivo> reajustes, List<ManualCierre> cierreManuales) {
        ConteoDecisiones conteo = conteoDeLaSemana(decisiones, cierre.semana());
        String queFunciono = queFunciono(cierre, conteo, cierreManuales);
        return guardarResumenSemanal(ResumenSemanal.de(
                cierre.objetivo(), cierre.semana(), cierre.medidor().resultado(), conteo, valoresAlDomingo,
                queFunciono, reajustes, cierreManuales, cierre.ahora()));
    }

    private static ConteoDecisiones conteoDeLaSemana(List<DecisionObjetivo> decisiones, LocalDate semana) {
        return ConteoDecisiones.de(decisiones.stream()
                .filter(decision -> decision.semanaInicio().equals(semana))
                .toList());
    }

    private static String queFunciono(Cierre cierre, ConteoDecisiones conteo, List<ManualCierre> manuales) {
        String deLaSemana = TextoResumen.queFunciono(
                cierre.medidor().resultado(), resultadoDe(cierre.anteriores(), cierre.semana().minusWeeks(1)),
                conteo, cierre.objetivo().getMeta(), cierre.alcanzado(), cierre.sinVentas());
        return String.join(" ", deLaSemana, TextoResumen.manuales(manuales)).strip();
    }

    /** Los manuales en curso que ya empezaron a esa semana: los que se renuevan o quedan sin respuesta. */
    private static List<MiniObjetivo> manualesEnCurso(List<MiniObjetivo> minis, LocalDate semana) {
        return minis.stream()
                .filter(MiniObjetivo::esManual)
                .filter(MiniObjetivo::estaEnCurso)
                .filter(mini -> !mini.getSemanaInicio().isAfter(semana))
                .toList();
    }

    private static List<ManualCierre> cierresDeManuales(
            List<MiniObjetivo> manuales, List<RespuestaMiniObjetivo> respuestas) {
        return manuales.stream()
                .map(manual -> respuestaDe(manual, respuestas)
                        .map(respuesta -> ManualCierre.respondido(manual, respuesta))
                        .orElseGet(() -> ManualCierre.sinRespuesta(manual)))
                .toList();
    }

    private static Optional<RespuestaMiniObjetivo> respuestaDe(
            MiniObjetivo mini, List<RespuestaMiniObjetivo> respuestas) {
        return respuestas.stream().filter(respuesta -> respuesta.miniObjetivoId().equals(mini.getId())).findFirst();
    }

    private static List<Renovable> renovables(List<MiniObjetivo> manuales, List<RespuestaMiniObjetivo> respuestas) {
        return manuales.stream()
                .flatMap(manual -> respuestaDe(manual, respuestas).map(respuesta -> new Renovable(manual, respuesta))
                        .stream())
                .toList();
    }

    /**
     * Lo que la IA redactó para renovar cada manual respondido de la semana, por mini-objetivo. Se pide antes de abrir la
     * transacción y sólo si hay algo que renovar. Sin IA (apagada, sin acceso, sin reserva del día o si falla) queda
     * vacío y cada acción lleva su texto de reserva.
     */
    private Map<MiniObjetivoId, TextoMiniObjetivo> textosDeRenovacion(
            NegocioId negocioId, Objetivo objetivo, LocalDate semana, OffsetDateTime ahora) {
        if (!iaHabilitada || objetivo.estaAlcanzado()) {
            return Map.of();
        }
        List<Renovable> renovables = renovablesDe(negocioId, objetivo, semana);
        if (renovables.isEmpty()) {
            return Map.of();
        }
        return redactarConIa(negocioId, renovables, ahora);
    }

    private List<Renovable> renovablesDe(NegocioId negocioId, Objetivo objetivo, LocalDate semana) {
        List<MiniObjetivo> minis = obtenerMinis(negocioId, objetivo);
        return renovables(manualesEnCurso(minis, semana), obtenerRespuestas(negocioId, objetivo));
    }

    private Map<MiniObjetivoId, TextoMiniObjetivo> redactarConIa(
            NegocioId negocioId, List<Renovable> renovables, OffsetDateTime ahora) {
        try {
            return redactarSiHayReservaDiaria(negocioId, renovables, ahora);
        } catch (RuntimeException e) {
            log.warn("No se pudieron redactar las renovaciones con IA; se usa el texto de reserva", e);
            return Map.of();
        }
    }

    private Map<MiniObjetivoId, TextoMiniObjetivo> redactarSiHayReservaDiaria(
            NegocioId negocioId, List<Renovable> renovables, OffsetDateTime ahora) {
        exigirAccesoALaIa(negocioId);
        LocalDate fecha = ZonaNegocio.dia(ahora);
        if (!reservarConsumoDiario(negocioId, fecha, ahora)) {
            return Map.of();
        }
        RedaccionObjetivosIa redaccion = pedirRedaccion(renovables);
        registrarConsumo(negocioId, fecha, redaccion, ahora);
        return textosPorMiniObjetivo(renovables, redaccion);
    }

    private static Map<MiniObjetivoId, TextoMiniObjetivo> textosPorMiniObjetivo(
            List<Renovable> renovables, RedaccionObjetivosIa redaccion) {
        Map<MiniObjetivoId, TextoMiniObjetivo> textos = new HashMap<>();
        for (int indice = 0; indice < renovables.size(); indice++) {
            textos.put(renovables.get(indice).mini().getId(), redaccion.textos().get(indice));
        }
        return textos;
    }

    /**
     * Renueva cada manual respondido según lo que contó la persona: el nuevo ocupa el mismo lugar desde la semana
     * siguiente y el viejo queda logrado (si lo hizo) o reemplazado. Un manual sin respuesta no se toca.
     */
    private List<ReajusteMiniObjetivo> renovar(Cierre cierre, List<Renovable> renovables) {
        return renovables.stream().map(renovable -> renovarManual(cierre, renovable)).toList();
    }

    private ReajusteMiniObjetivo renovarManual(Cierre cierre, Renovable renovable) {
        MiniObjetivo anterior = renovable.mini();
        AccionRenovacion accion = AccionRenovacion.de(renovable.respuesta().hizo(), renovable.respuesta().quePaso());
        Optional<AccionManual> deLaIa = accionRedactadaPorLaIa(cierre, anterior);
        AccionManual texto = deLaIa.orElseGet(() -> accion.textoDeReserva(anterior.getTitulo()));
        MiniObjetivo nuevo = MiniObjetivo.manual(cierre.objetivo(), anterior.getPosicion(), texto,
                deLaIa.isPresent(), cierre.semanaSiguiente(), cierre.ahora());
        renovable.respuesta().hizo().cerrarAnterior(anterior, nuevo.getId(), cierre.ahora());
        guardarMini(anterior);
        guardarMini(nuevo);
        return ReajusteMiniObjetivo.de(anterior, nuevo, accion.tipoReajuste(),
                accion.motivo(anterior.getTitulo(), nuevo.getTitulo()), cierre.ahora());
    }

    private static Optional<AccionManual> accionRedactadaPorLaIa(Cierre cierre, MiniObjetivo anterior) {
        return Optional.ofNullable(cierre.textos().get(anterior.getId()))
                .flatMap(texto -> AccionManual.de(texto.titulo(), texto.justificacion()));
    }

    /** El valor al domingo de los mini-objetivos en curso o logrados, para compararlo en los cierres siguientes. */
    private static List<CierreMini> cierres(List<MiniObjetivo> minis, MedidorMiniObjetivo medidor) {
        return minis.stream()
                .filter(MiniObjetivo::esMedido)
                .filter(mini -> mini.estaEnCurso() || mini.esperaReajuste())
                .flatMap(mini -> valorDe(medidor, mini).map(valor -> new CierreMini(mini.getId(), valor)).stream())
                .toList();
    }

    private static Optional<BigDecimal> valorDe(MedidorMiniObjetivo medidor, MiniObjetivo mini) {
        return medidor.valorDe(mini.getTipo(), mini.getRegistroId(), mini.getReferencia());
    }

    private Map<AvisoId, Aviso> avisosDe(List<DecisionObjetivo> decisiones, NegocioId negocioId) {
        List<AvisoId> ids = decisiones.stream().map(DecisionObjetivo::avisoId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return obtenerAvisos(ids, negocioId).stream().collect(Collectors.toMap(Aviso::getId, Function.identity()));
    }

    private static Optional<BigDecimal> resultadoDe(List<ResumenSemanal> resumenes, LocalDate semana) {
        return resumenes.stream()
                .filter(resumen -> resumen.semanaInicio().equals(semana))
                .map(ResumenSemanal::resultado)
                .findFirst();
    }

    private List<NegocioId> obtenerTanda(int pagina) {
        return objetivoRepository.findNegocioIdsConObjetivos(pagina, tamanoTanda);
    }

    private List<Objetivo> obtenerObjetivos(NegocioId negocioId) {
        return objetivoRepository.findAllByNegocioId(negocioId);
    }

    private List<ResumenSemanal> obtenerResumenes(Objetivo objetivo) {
        return resumenSemanalRepository.findAllByObjetivoId(objetivo.getId());
    }

    private MedidorMiniObjetivo medirElNegocio(NegocioId negocioId, LocalDate dia) {
        return propuestaMiniObjetivosService.medidor(negocioId, dia);
    }

    private List<CandidatoMiniObjetivo> obtenerCandidatos(Cierre cierre) {
        return propuestaMiniObjetivosService.candidatos(cierre.negocioId(), cierre.medidor());
    }

    private List<MiniObjetivo> obtenerMinis(Cierre cierre) {
        return obtenerMinis(cierre.negocioId(), cierre.objetivo());
    }

    private List<MiniObjetivo> obtenerMinis(NegocioId negocioId, Objetivo objetivo) {
        return miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
    }

    private List<DecisionObjetivo> obtenerDecisiones(Cierre cierre) {
        return decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(
                cierre.objetivo().getId(), cierre.negocioId());
    }

    private List<RespuestaMiniObjetivo> obtenerRespuestas(Cierre cierre) {
        return obtenerRespuestas(cierre.negocioId(), cierre.objetivo());
    }

    private List<RespuestaMiniObjetivo> obtenerRespuestas(NegocioId negocioId, Objetivo objetivo) {
        return respuestaMiniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivo.getId(), negocioId);
    }

    private List<Aviso> obtenerAvisos(List<AvisoId> ids, NegocioId negocioId) {
        return avisoRepository.findAllByIdsAndNegocioId(ids, negocioId);
    }

    private void exigirAccesoALaIa(NegocioId negocioId) {
        controlPlanService.exigirIa(negocioId);
    }

    private boolean reservarConsumoDiario(NegocioId negocioId, LocalDate fecha, OffsetDateTime ahora) {
        return consumoSamiRepository.reservar(negocioId, fecha, limiteDiarioIa, ahora);
    }

    private RedaccionObjetivosIa pedirRedaccion(List<Renovable> renovables) {
        return renovacionManualesIaPort.renovar(renovables.stream()
                .map(renovable -> PedidoRenovacion.de(renovable.mini(), renovable.respuesta()))
                .toList());
    }

    private void registrarConsumo(
            NegocioId negocioId, LocalDate fecha, RedaccionObjetivosIa redaccion, OffsetDateTime ahora) {
        consumoSamiRepository.registrar(negocioId, fecha, redaccion.consumo(), ahora);
    }

    private ResumenSemanal guardarResumenSemanal(ResumenSemanal resumen) {
        return resumenSemanalRepository.save(resumen);
    }

    private MiniObjetivo guardarMini(MiniObjetivo mini) {
        return miniObjetivoRepository.save(mini);
    }

    /**
     * Los reajustes de un cierre: lleva qué mini-objetivos quedan en curso y cuáles ya se usaron, para que dos lugares
     * no reciban el mismo candidato. Los nuevos empiezan la semana siguiente.
     */
    private final class Reajuste {

        private final Cierre cierre;

        private final List<MiniObjetivo> minis;

        private final List<DecisionObjetivo> decisiones;

        private final List<CandidatoMiniObjetivo> candidatos;

        private final List<MiniObjetivo> enCurso;

        private final List<MiniObjetivo> usados;

        private final Map<AvisoId, Aviso> avisos;

        private final Optional<ResumenSemanal> dosAtras;

        private Reajuste(Cierre cierre, List<MiniObjetivo> minis, List<DecisionObjetivo> decisiones) {
            this.cierre = cierre;
            this.minis = minis;
            this.decisiones = decisiones;
            this.candidatos = obtenerCandidatos(cierre);
            this.enCurso = new ArrayList<>(minis.stream().filter(MiniObjetivo::estaEnCurso).toList());
            this.usados = new ArrayList<>(ReglasReajuste.usados(minis, cierre.semana()));
            this.avisos = avisosDe(decisiones, cierre.negocioId());
            this.dosAtras = resumenDeHaceDosSemanas();
        }

        List<ReajusteMiniObjetivo> reajustarMedidos() {
            return minis.stream()
                    .filter(MiniObjetivo::esMedido)
                    .map(this::reajustarMedido)
                    .flatMap(Optional::stream)
                    .toList();
        }

        private Optional<ReajusteMiniObjetivo> reajustarMedido(MiniObjetivo mini) {
            Optional<BigDecimal> valor = valorDe(cierre.medidor(), mini);
            return ReglasReajuste.decidir(
                            mini,
                            valor,
                            dosAtras.flatMap(resumen -> resumen.valorDe(mini.getId())),
                            ReglasReajuste.decisionSinEfecto(mini, decisiones, avisos),
                            cierre.semana())
                    .flatMap(motivo -> aplicar(mini, motivo, valor.orElse(mini.getValorActual())));
        }

        private Optional<ResumenSemanal> resumenDeHaceDosSemanas() {
            return cierre.anteriores().stream()
                    .filter(resumen -> resumen.semanaInicio().equals(cierre.semana().minusWeeks(2)))
                    .findFirst();
        }

        /**
         * Un logrado sigue con una meta más alta; si ya no puede pedir más, o por cualquier otro motivo, se cambia por
         * el mejor candidato. Sin candidato se mantiene. Un lugar que ya tiene a otro en curso no se toca.
         */
        private Optional<ReajusteMiniObjetivo> aplicar(
                MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre) {
            if (!mini.estaEnCurso() && lugarOcupado(mini)) {
                return Optional.empty();
            }
            return subirLaMeta(mini, motivo, valorAlCierre)
                    .or(() -> cambiarPorOtro(mini, motivo, valorAlCierre));
        }

        private Optional<ReajusteMiniObjetivo> subirLaMeta(
                MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre) {
            if (!motivo.puedeSubirLaMeta()) {
                return Optional.empty();
            }
            return mini.metaSiguiente(valorAlCierre).map(meta -> subirMeta(mini, valorAlCierre, meta));
        }

        private Optional<ReajusteMiniObjetivo> cambiarPorOtro(
                MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre) {
            return ReglasReajuste.reemplazo(candidatos, mini, motivo, enCurso, usados)
                    .map(candidato -> reemplazar(mini, motivo, valorAlCierre, candidato));
        }

        private ReajusteMiniObjetivo subirMeta(MiniObjetivo mini, BigDecimal valorAlCierre, BigDecimal meta) {
            String motivo = TextoResumen.metaSubida(mini.getTipo(), valorAlCierre, meta);
            MiniObjetivo nuevo = MiniObjetivo.nuevo(cierre.objetivo(), mini.getPosicion(),
                    mini.conMeta(valorAlCierre, meta, motivo), cierre.semanaSiguiente(), cierre.ahora());
            mini.lograrYSeguirCon(nuevo.getId(), cierre.ahora());
            return guardar(mini, nuevo, TipoReajuste.SUBIR_META, motivo);
        }

        private ReajusteMiniObjetivo reemplazar(
                MiniObjetivo mini, MotivoReajuste motivo, BigDecimal valorAlCierre, CandidatoMiniObjetivo candidato) {
            MiniObjetivo nuevo = MiniObjetivo.nuevo(
                    cierre.objetivo(), mini.getPosicion(), candidato, cierre.semanaSiguiente(), cierre.ahora());
            motivo.soltar(mini, nuevo.getId(), cierre.ahora());
            return guardar(mini, nuevo, motivo.tipoAlReemplazar(),
                    TextoResumen.motivo(motivo, mini, valorAlCierre, nuevo));
        }

        /** Primero se libera el lugar del anterior y recién después entra el nuevo: un lugar lo ocupa uno solo. */
        private ReajusteMiniObjetivo guardar(
                MiniObjetivo anterior, MiniObjetivo nuevo, TipoReajuste tipo, String motivo) {
            guardarMini(anterior);
            MiniObjetivo guardado = guardarMini(nuevo);
            enCurso.removeIf(otro -> otro.getId().equals(anterior.getId()));
            enCurso.add(guardado);
            usados.add(guardado);
            return ReajusteMiniObjetivo.de(anterior, guardado, tipo, motivo, cierre.ahora());
        }

        private boolean lugarOcupado(MiniObjetivo mini) {
            return enCurso.stream().anyMatch(otro -> otro.getPosicion() == mini.getPosicion());
        }
    }
}
