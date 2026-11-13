package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
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

    public ConsultarPlanesResponse execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        ConsultaPlanes consulta = transaccionPort.ejecutar(() -> consultar(usuarioId, negocioId, ahora));
        return ConsultarPlanesResponse.de(consulta, ahora);
    }

    /** Una única vista de la cuenta frente a activaciones, consumo y vencimientos concurrentes. */
    private ConsultaPlanes consultar(UsuarioId usuarioId, NegocioId negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearCuenta(usuarioId);
        Plan gratuito = planRepository.obtener(CodigoPlan.GRATUITO);
        Plan premium = planRepository.obtener(CodigoPlan.PREMIUM);
        Suscripcion suscripcion = suscripcionRepository.obtener(usuarioId);
        boolean vigente = suscripcion.estaVigente(ahora);
        Plan actual = vigente ? planRepository.findById(suscripcion.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado")) : gratuito;
        actual.validarLimitesConfigurados();
        PruebaPremium prueba = pruebaPremiumRepository.findByUsuarioId(usuarioId).orElse(null);
        boolean pruebaActiva = vigente && suscripcion.getEstado() == EstadoSuscripcion.PRUEBA;
        boolean puedeIniciar = prueba == null && !(vigente && actual.getCodigo() == CodigoPlan.PREMIUM)
                && Boolean.TRUE.equals(premium.getLimitesConfigurados())
                && !renovacionPremiumRepository.existeAbierta(usuarioId);
        long negocios = usoPlanRepository.contarNegocios(usuarioId);
        long registros = usoPlanRepository.contarRegistros(negocioId);
        if (pruebaActiva) {
            return consultaEnPrueba(gratuito, premium, actual, suscripcion, prueba, negocios, registros);
        }
        LocalDate primerDia = ZonaNegocio.primerDiaDelMes(ahora);
        OffsetDateTime inicioMes = ZonaNegocio.inicioDelDia(primerDia);
        OffsetDateTime finMes = ZonaNegocio.inicioDelDia(primerDia.plusMonths(1));
        long paginas = usoPlanRepository.paginasEnPeriodo(usuarioId, primerDia, primerDia.plusMonths(1));
        return new ConsultaPlanes(List.of(gratuito, premium), actual, suscripcion, prueba, false, puedeIniciar,
                negocios, registros, paginas, actual.getLimitePaginasIaMensuales(), "MENSUAL", inicioMes, finMes);
    }

    /** Durante la prueba el cupo de IA es el total de la prueba y su período, el de la prueba. */
    private ConsultaPlanes consultaEnPrueba(Plan gratuito, Plan premium, Plan actual, Suscripcion suscripcion,
                                            PruebaPremium prueba, long negocios, long registros) {
        if (prueba == null) {
            throw new ResourceNotFoundException("Prueba no encontrada");
        }
        return new ConsultaPlanes(List.of(gratuito, premium), actual, suscripcion, prueba, true, false,
                negocios, registros, prueba.paginasIa(), PruebaPremium.LIMITE_PAGINAS_IA,
                "PRUEBA", prueba.inicioEn(), prueba.finEn());
    }
}
