package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ConsultarPlanes {

    private final NegocioRepository negocioRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final PlanRepository planRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    private static final String PERIODO_MENSUAL = "MENSUAL";
    private static final String PERIODO_DE_PRUEBA = "PRUEBA";

    /** Lo que se sabe del negocio antes de armar la consulta: sus planes, su suscripción y su prueba. */
    private record Situacion(Plan gratuito, Plan premium, Suscripcion suscripcion, boolean vigente, Plan actual,
                             PruebaPremium prueba) {

        List<Plan> planes() {
            return List.of(gratuito, premium);
        }

        boolean estaEnPrueba() {
            return vigente && suscripcion.estaEnPrueba();
        }

        boolean tienePremiumVigente() {
            return vigente && actual.esPremium();
        }

        boolean noUsoLaPrueba() {
            return prueba == null;
        }
    }

    /** Lo que el negocio ya consumió: proyectos, registros y objetivos con IA del mes. */
    private record Consumo(long proyectos, long registros, long objetivosIa) {
    }

    public ConsultarPlanesResponse execute(UsuarioId usuarioId) {
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ConsultaPlanes consulta = consultarEnUnaTransaccion(negocioId, ahora);
        return ConsultarPlanesResponse.de(consulta, ahora);
    }

    private ConsultaPlanes consultarEnUnaTransaccion(NegocioId negocioId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> consultar(negocioId, ahora));
    }

    /** Una única vista del negocio frente a activaciones, consumo y vencimientos concurrentes. */
    private ConsultaPlanes consultar(NegocioId negocioId, OffsetDateTime ahora) {
        bloquearNegocio(negocioId);
        Situacion situacion = situacionDe(negocioId, ahora);
        Consumo consumo = consumoDe(negocioId, ahora);
        if (situacion.estaEnPrueba()) {
            return consultaEnPrueba(situacion, consumo);
        }
        return consultaMensual(situacion, negocioId, consumo, ahora);
    }

    private Consumo consumoDe(NegocioId negocioId, OffsetDateTime ahora) {
        return new Consumo(contarProyectos(negocioId), contarRegistros(negocioId),
                contarObjetivosIa(negocioId, ZonaNegocio.primerDiaDelMes(ahora)));
    }

    private Situacion situacionDe(NegocioId negocioId, OffsetDateTime ahora) {
        Plan gratuito = obtenerPlan(CodigoPlan.GRATUITO);
        Plan premium = obtenerPlan(CodigoPlan.PREMIUM);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        boolean vigente = suscripcion.estaVigente(ahora);
        Plan actual = vigente ? buscarPlan(suscripcion) : gratuito;
        actual.validarLimitesConfigurados();
        return new Situacion(gratuito, premium, suscripcion, vigente, actual, buscarPrueba(negocioId).orElse(null));
    }

    /** Durante la prueba el cupo de IA es el total de la prueba y su período, el de la prueba. */
    private ConsultaPlanes consultaEnPrueba(Situacion situacion, Consumo consumo) {
        PruebaPremium prueba = Optional.ofNullable(situacion.prueba())
                .orElseThrow(() -> new ResourceNotFoundException("Prueba no encontrada"));
        return new ConsultaPlanes(situacion.planes(), situacion.actual(), situacion.suscripcion(), prueba, true,
                false, consumo.proyectos(), consumo.registros(), prueba.paginasIa(),
                PruebaPremium.LIMITE_PAGINAS_IA, PERIODO_DE_PRUEBA, prueba.inicioEn(), prueba.finEn(),
                consumo.objetivosIa(), situacion.actual().getLimiteObjetivosIaMensuales());
    }

    private ConsultaPlanes consultaMensual(Situacion situacion, NegocioId negocioId, Consumo consumo,
                                           OffsetDateTime ahora) {
        LocalDate primerDia = ZonaNegocio.primerDiaDelMes(ahora);
        LocalDate primerDiaDelMesSiguiente = primerDia.plusMonths(1);
        long paginas = contarPaginas(negocioId, primerDia, primerDiaDelMesSiguiente);
        return new ConsultaPlanes(situacion.planes(), situacion.actual(), situacion.suscripcion(),
                situacion.prueba(), false, puedeIniciarPrueba(situacion, negocioId), consumo.proyectos(),
                consumo.registros(), paginas, situacion.actual().getLimitePaginasIaMensuales(), PERIODO_MENSUAL,
                ZonaNegocio.inicioDelDia(primerDia), ZonaNegocio.inicioDelDia(primerDiaDelMesSiguiente),
                consumo.objetivosIa(), situacion.actual().getLimiteObjetivosIaMensuales());
    }

    private boolean puedeIniciarPrueba(Situacion situacion, NegocioId negocioId) {
        return situacion.noUsoLaPrueba() && !situacion.tienePremiumVigente()
                && situacion.premium().tieneLimitesConfigurados() && !hayUnaContratacionEnCurso(negocioId);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Plan obtenerPlan(CodigoPlan codigo) {
        return planRepository.obtener(codigo);
    }

    private Plan buscarPlan(Suscripcion suscripcion) {
        return planRepository.findById(suscripcion.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    private Suscripcion obtenerSuscripcion(NegocioId negocioId) {
        return suscripcionRepository.obtener(negocioId);
    }

    private Optional<PruebaPremium> buscarPrueba(NegocioId negocioId) {
        return pruebaPremiumRepository.findByNegocioId(negocioId);
    }

    private boolean hayUnaContratacionEnCurso(NegocioId negocioId) {
        return renovacionPremiumRepository.existeAbierta(negocioId);
    }

    private long contarProyectos(NegocioId negocioId) {
        return usoPlanRepository.contarProyectos(negocioId);
    }

    private long contarRegistros(NegocioId negocioId) {
        return usoPlanRepository.contarRegistros(negocioId);
    }

    private long contarObjetivosIa(NegocioId negocioId, LocalDate primerDia) {
        return usoPlanRepository.objetivosIaEnPeriodo(negocioId, ZonaNegocio.inicioDelDia(primerDia),
                ZonaNegocio.inicioDelDia(primerDia.plusMonths(1)));
    }

    private long contarPaginas(NegocioId negocioId, LocalDate desde, LocalDate hasta) {
        return usoPlanRepository.paginasEnPeriodo(negocioId, desde, hasta);
    }
}
