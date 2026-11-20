package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Las reglas del cierre semanal para cada mini-objetivo. Una sola por mini-objetivo y semana, en este orden: lo
 * logrado sube su meta; lo quitado o lo que ya no se puede medir se cambia; lo que tuvo una decisión sin efecto prueba
 * otro camino; lo que no avanzó en dos cierres se cambia. Si nada aplica, se mantiene.
 */
public final class ReglasReajuste {

    /** Cuántas semanas tiene que pasar para volver a proponer un mini-objetivo que ya se usó. */
    public static final int SEMANAS_SIN_REPETIR = 4;

    private ReglasReajuste() {
    }

    /**
     * Qué hacer con el mini-objetivo al cerrar la semana que empieza el {@code semana}; vacío si se mantiene. Uno que
     * empezó esa misma semana sólo puede subir su meta.
     *
     * @param valorAlCierre        el valor de su métrica al domingo; vacío si ya no se puede medir
     * @param valorDosCierresAtras el valor que tenía en el cierre de dos semanas antes; vacío si no lo hay
     * @param decisionSinEfecto    si se aplicó un aviso vinculado y, medido, las ventas no subieron
     */
    public static Optional<MotivoReajuste> decidir(
            MiniObjetivo mini, Optional<BigDecimal> valorAlCierre, Optional<BigDecimal> valorDosCierresAtras,
            boolean decisionSinEfecto, LocalDate semana) {
        if (mini.getEstado() == EstadoMiniObjetivo.ABANDONADO || mini.getEstado() == EstadoMiniObjetivo.LOGRADO) {
            return mini.esperaReajuste() ? Optional.of(motivoDeEspera(mini)) : Optional.empty();
        }
        if (!mini.estaEnCurso()) {
            return Optional.empty();
        }
        if (valorAlCierre.isEmpty()) {
            return mini.getTipo().esDeProducto() ? Optional.of(MotivoReajuste.PRODUCTO_FUERA) : Optional.empty();
        }
        if (mini.cumpleCon(valorAlCierre.get())) {
            return Optional.of(MotivoReajuste.LOGRADO);
        }
        if (!mini.getSemanaInicio().isBefore(semana)) {
            return Optional.empty();
        }
        if (decisionSinEfecto) {
            return Optional.of(MotivoReajuste.DECISION_SIN_EFECTO);
        }
        if (valorDosCierresAtras.isPresent() && !mejora(mini, valorDosCierresAtras.get(), valorAlCierre.get())) {
            return Optional.of(MotivoReajuste.SIN_AVANCE);
        }
        return Optional.empty();
    }

    /**
     * Si alguna decisión de aplicar un aviso vinculada al mini-objetivo ya se midió y las ventas no subieron.
     * {@code avisos} son los avisos de las decisiones, por id; uno que falta se borró y no se mide.
     */
    public static boolean decisionSinEfecto(
            MiniObjetivo mini, List<DecisionObjetivo> decisiones, Map<AvisoId, Aviso> avisos) {
        return decisiones.stream()
                .filter(decision -> mini.getId().equals(decision.miniObjetivoId()))
                .map(decision -> EfectoDecision.de(decision, avisoDe(decision, avisos)))
                .anyMatch(efecto -> efecto.estado() == EstadoEfecto.MEDIDO
                        && efecto.deltaIngresoSemanal().signum() <= 0);
    }

    /**
     * Los mini-objetivos del objetivo que no se pueden volver a proponer al cerrar la semana del {@code semana}: los
     * que siguen en curso y los que empezaron en las últimas {@link #SEMANAS_SIN_REPETIR} semanas.
     */
    public static List<MiniObjetivo> usados(Collection<MiniObjetivo> minis, LocalDate semana) {
        LocalDate desde = semana.minusWeeks(SEMANAS_SIN_REPETIR - 1L);
        return minis.stream()
                .filter(mini -> mini.estaEnCurso() || !mini.getSemanaInicio().isBefore(desde))
                .toList();
    }

    /**
     * El candidato para ocupar el lugar de {@code mini}: el de mayor aporte que no repite uno usado ni choca con el
     * tipo o el producto de los otros en curso. Para probar otro camino tiene que ser de otro tipo.
     */
    public static Optional<CandidatoMiniObjetivo> reemplazo(
            List<CandidatoMiniObjetivo> candidatos, MiniObjetivo mini, MotivoReajuste motivo,
            Collection<MiniObjetivo> enCurso, Collection<MiniObjetivo> usados) {
        boolean otroTipo = motivo.tipo() == TipoReajuste.PROBAR_OTRO_CAMINO;
        List<CandidatoMiniObjetivo> posibles = candidatos.stream()
                .filter(candidato -> !candidato.esElMismo(mini.getTipo(), mini.getRegistroId(), mini.getReferencia()))
                .filter(candidato -> usados.stream().noneMatch(usado -> candidato.esElMismo(
                        usado.getTipo(), usado.getRegistroId(), usado.getReferencia())))
                .filter(candidato -> !otroTipo || candidato.tipo() != mini.getTipo())
                .toList();
        List<MiniObjetivo> ocupados = enCurso.stream()
                .filter(otro -> !otro.getId().equals(mini.getId()))
                .toList();
        return CandidatosMiniObjetivo.elegir(posibles, ocupados, 1).stream().findFirst();
    }

    /** El aviso de la decisión; {@code null} si no es sobre un aviso o el aviso se borró. */
    private static Aviso avisoDe(DecisionObjetivo decision, Map<AvisoId, Aviso> avisos) {
        return decision.avisoId() == null ? null : avisos.get(decision.avisoId());
    }

    private static MotivoReajuste motivoDeEspera(MiniObjetivo mini) {
        return mini.getEstado() == EstadoMiniObjetivo.LOGRADO ? MotivoReajuste.LOGRADO : MotivoReajuste.QUITADO;
    }

    private static boolean mejora(MiniObjetivo mini, BigDecimal antes, BigDecimal despues) {
        return mini.getTipo().sentido() == SentidoMiniObjetivo.SUBE
                ? despues.compareTo(antes) > 0
                : despues.compareTo(antes) < 0;
    }
}
