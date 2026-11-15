package com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.AccionManual;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoPropuesto;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.SemanaObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest.MiniElegido;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Guarda el objetivo que la persona armó con Sami, sin meta en pesos, con los mini-objetivos que aceptó. El tipo del
 * objetivo lo deduce el servidor de esos mini-objetivos. De cada medido se vuelve a calcular todo con los datos de
 * hoy: lo que mandó el frontend sólo dice cuál es, y uno que ya no está en el catálogo se rechaza. Un manual tiene que
 * cumplir las reglas de una acción.
 * <p>
 * Con {@code seguimientoManual} guarda el seguimiento de respaldo (cuando la IA no estuvo): no cuenta como armado con
 * Sami ni gasta cuota. Un objetivo armado con Sami gasta una unidad de la cuota mensual del plan, que no se devuelve si
 * después se borra; sin cuota responde {@code LIMITE_OBJETIVOS_IA} y no guarda nada.
 */
@RequiredArgsConstructor
public class GuardarObjetivoConSami {

    private static final String YA_NO_ESTA = "Ese mini-objetivo ya no está disponible: pedile otra propuesta a Sami";

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService;

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    /** Cómo se resuelve lo que eligió la persona según quién mide el mini-objetivo. */
    @FunctionalInterface
    private interface Resolucion {

        MiniObjetivoPropuesto resolver(
                MiniElegido elegido, List<CandidatoMiniObjetivo> catalogo, List<MiniObjetivoPropuesto> yaElegidos);
    }

    private static final Map<ModoMiniObjetivo, Resolucion> RESOLUCIONES = Map.of(
            ModoMiniObjetivo.MEDIDO, GuardarObjetivoConSami::medido,
            ModoMiniObjetivo.MANUAL, (elegido, catalogo, yaElegidos) -> manual(elegido));

    public ObjetivoItem execute(GuardarObjetivoConSamiRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LocalDate hoy = ZonaNegocio.dia(ahora);
        List<MiniObjetivoPropuesto> minis = minisAGuardar(request, negocioId, hoy);
        Objetivo objetivo = Objetivo.conSami(
                negocioId, request.texto(), TipoObjetivo.de(minis), !request.seguimientoManual(), ahora);
        return ObjetivoItem.de(guardarEnUnaTransaccion(objetivo, minis, hoy, ahora));
    }

    private List<MiniObjetivoPropuesto> minisAGuardar(
            GuardarObjetivoConSamiRequest request, NegocioId negocioId, LocalDate hoy) {
        if (request.seguimientoManual()) {
            return List.of(MiniObjetivoPropuesto.manual(AccionManual.seguimiento(request.texto()), false));
        }
        return resolver(negocioId, request.minis(), hoy);
    }

    /** Un objetivo armado con IA gasta una unidad de la cuota del mes, comprobada con el negocio bloqueado. */
    private Objetivo guardar(
            Objetivo nuevo, List<MiniObjetivoPropuesto> minis, LocalDate hoy, OffsetDateTime ahora) {
        gastarCuotaSiEsDeSami(nuevo, ahora);
        Objetivo guardado = guardarObjetivo(nuevo);
        registrarUsoSiEsDeSami(guardado, ahora);
        guardarMinis(guardado, minis, SemanaObjetivo.lunesDe(hoy), ahora);
        return guardado;
    }

    private void gastarCuotaSiEsDeSami(Objetivo objetivo, OffsetDateTime ahora) {
        if (objetivo.isArmadoConSami()) {
            controlPlanService.validarObjetivoIa(objetivo.getNegocioId(), ahora);
        }
    }

    private void registrarUsoSiEsDeSami(Objetivo objetivo, OffsetDateTime ahora) {
        if (objetivo.isArmadoConSami()) {
            usoPlanRepository.registrarObjetivoIa(objetivo.getNegocioId(), objetivo.getId(), ahora);
        }
    }

    private void guardarMinis(
            Objetivo objetivo, List<MiniObjetivoPropuesto> minis, LocalDate semana, OffsetDateTime ahora) {
        for (int indice = 0; indice < minis.size(); indice++) {
            guardarMini(minis.get(indice).aMiniObjetivo(objetivo, indice + 1, semana, ahora));
        }
    }

    /** @throws BusinessRuleException si no hay entre 1 y 3, uno medido no está o choca, o uno manual no es válido */
    private List<MiniObjetivoPropuesto> resolver(NegocioId negocioId, List<MiniElegido> elegidos, LocalDate hoy) {
        exigirEntreUnoYTres(elegidos);
        List<CandidatoMiniObjetivo> catalogo = catalogoSiHaceFalta(negocioId, elegidos, hoy);
        List<MiniObjetivoPropuesto> resueltos = new ArrayList<>();
        for (MiniElegido elegido : elegidos) {
            resueltos.add(RESOLUCIONES.get(elegido.modo()).resolver(elegido, catalogo, resueltos));
        }
        return List.copyOf(resueltos);
    }

    private static void exigirEntreUnoYTres(List<MiniElegido> elegidos) {
        if (elegidos == null || elegidos.isEmpty() || elegidos.size() > MiniObjetivo.MAXIMO_EN_CURSO) {
            throw new BusinessRuleException(
                    "Elegí entre 1 y " + MiniObjetivo.MAXIMO_EN_CURSO + " mini-objetivos para guardar");
        }
    }

    private List<CandidatoMiniObjetivo> catalogoSiHaceFalta(
            NegocioId negocioId, List<MiniElegido> elegidos, LocalDate hoy) {
        if (elegidos.stream().noneMatch(elegido -> elegido.modo().usaElCatalogo())) {
            return List.of();
        }
        return obtenerCatalogo(negocioId, hoy);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<CandidatoMiniObjetivo> obtenerCatalogo(NegocioId negocioId, LocalDate hoy) {
        return propuestaMiniObjetivosService.candidatos(negocioId, hoy).candidatos();
    }

    private Objetivo guardarEnUnaTransaccion(
            Objetivo objetivo, List<MiniObjetivoPropuesto> minis, LocalDate hoy, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> guardar(objetivo, minis, hoy, ahora));
    }

    private Objetivo guardarObjetivo(Objetivo objetivo) {
        return objetivoRepository.save(objetivo);
    }

    private void guardarMini(MiniObjetivo mini) {
        miniObjetivoRepository.save(mini);
    }

    private static MiniObjetivoPropuesto medido(
            MiniElegido elegido, List<CandidatoMiniObjetivo> catalogo, List<MiniObjetivoPropuesto> yaElegidos) {
        CandidatoMiniObjetivo candidato = catalogo.stream()
                .filter(posible -> posible.esElMismo(elegido.tipo(), elegido.registroId(), elegido.referencia()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(YA_NO_ESTA));
        if (yaElegidos.stream().anyMatch(otro -> otro.esMedido() && candidato.chocaCon(otro.candidato()))) {
            throw new BusinessRuleException("Dos mini-objetivos no pueden medir lo mismo");
        }
        return MiniObjetivoPropuesto.medido(candidato);
    }

    private static MiniObjetivoPropuesto manual(MiniElegido elegido) {
        AccionManual accion = AccionManual.de(elegido.titulo(), elegido.justificacion())
                .orElseThrow(() -> new BusinessRuleException(
                        "El texto de la acción no es válido: tiene que tener un título y una justificación cortos "
                                + "y sin importes ni porcentajes"));
        return MiniObjetivoPropuesto.manual(accion, true);
    }
}
