package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

@RequiredArgsConstructor
public class ControlPlanService {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final AccesoContenidoRepository accesoContenidoRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final TransaccionPort transaccionPort;

    private static final int LIMITE_GRATUITO = 3;

    public void validarAltaProyecto(NegocioId negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            plan.validarProyectos(usoPlanRepository.contarProyectos(negocioId));
        });
    }

    public void validarAltaRegistros(NegocioId negocioId, long cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            plan.validarRegistros(usoPlanRepository.contarRegistros(negocioId), cantidad);
        });
    }

    public void validarPaginasIa(NegocioId negocioId, int cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            Optional<PruebaPremium> prueba = pruebaVigente(negocioId, ahora);
            if (prueba.isPresent()) {
                prueba.get().validarPaginas(cantidad);
                return;
            }
            LocalDate mes = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
            plan.validarPaginasIa(usoPlanRepository.paginasEnPeriodo(negocioId, mes, mes.plusMonths(1)), cantidad);
        });
    }

    public void validarFuncion(NegocioId negocioId, FuncionPlan funcion, OffsetDateTime ahora) {
        vigente(negocioId, ahora).validarFuncion(funcion);
    }

    private boolean esPremium(NegocioId negocioId, OffsetDateTime ahora) {
        return vigente(negocioId, ahora).getCodigo() == CodigoPlan.PREMIUM;
    }

    /**
     * Admite un intento de lectura con IA: valida la cuota mensual o de la prueba, hace la {@code reserva} diaria y, si
     * entró, la descuenta de la prueba. Todo en una transacción con el negocio bloqueado.
     *
     * @return si la reserva diaria entró
     */
    public boolean reservarPaginasIa(NegocioId negocioId, int cantidad, OffsetDateTime ahora, BooleanSupplier reserva) {
        return transaccionPort.ejecutar(() -> {
            validarPaginasIa(negocioId, cantidad, ahora);
            boolean reservada = reserva.getAsBoolean();
            if (reservada) {
                registrarPaginasPrueba(negocioId, cantidad, ahora);
            }
            return reservada;
        });
    }

    /**
     * La reserva de una foto leída en el momento (sin pasar por el lote): además del cupo mensual del plan cuenta
     * contra el límite diario de fotos. Es la misma regla para la app y para WhatsApp.
     *
     * @throws RestriccionPlanException si se agotó el límite diario; el mensual lo informa {@link #validarPaginasIa}
     */
    public void reservarPaginaIaInmediata(NegocioId negocioId, int limiteDiario, OffsetDateTime ahora) {
        LocalDate dia = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        boolean reservada = reservarPaginasIa(negocioId, 1, ahora,
                () -> usoPlanRepository.registrarPaginaInmediata(negocioId, dia, limiteDiario, ahora));
        if (!reservada) {
            throw new RestriccionPlanException("LIMITE_IA_DIARIO", PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        }
    }

    /** Se cobra al admitir un intento. También cuentan errores y timeouts, sin duplicar al finalizar el lote. */
    private void registrarPaginasPrueba(NegocioId negocioId, int cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            pruebaVigente(negocioId, ahora)
                    .ifPresent(prueba -> pruebaPremiumRepository.save(prueba.consumir(cantidad)));
        });
    }

    private Optional<PruebaPremium> pruebaVigente(NegocioId negocioId, OffsetDateTime ahora) {
        Suscripcion suscripcion = suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        if (suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && suscripcion.estaVigente(ahora)) {
            return Optional.of(pruebaPremiumRepository.findByNegocioId(negocioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prueba no encontrada")));
        }
        return Optional.empty();
    }

    private Plan vigente(NegocioId negocioId, OffsetDateTime ahora) {
        Suscripcion suscripcion = suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        Plan plan = suscripcion.estaVigente(ahora)
                ? planRepository.findById(suscripcion.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"))
                : planRepository.findByCodigo(CodigoPlan.GRATUITO)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan gratuito no encontrado"));
        plan.validarLimitesConfigurados();
        return plan;
    }

    public AccesoContenidoItem seleccionar(NegocioId negocioId, TipoContenidoPlan tipo, List<String> claves,
                                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            LocalDate periodo = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
            OffsetDateTime fin = periodo.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            List<String> candidatas = claves.stream().distinct().toList();
            if (esPremium(negocioId, ahora)) {
                return new AccesoContenidoItem(candidatas, 0, null, candidatas.size(), 0, fin);
            }
            Set<String> conocidas = accesoContenidoRepository.autorizadas(negocioId, tipo, periodo);
            List<String> permitidas = new ArrayList<>();
            for (String clave : candidatas) {
                if (conocidas.contains(clave)) {
                    permitidas.add(clave);
                } else if (conocidas.size() < LIMITE_GRATUITO) {
                    accesoContenidoRepository.registrar(negocioId, tipo, periodo, clave, ahora);
                    conocidas.add(clave);
                    permitidas.add(clave);
                }
            }
            return new AccesoContenidoItem(List.copyOf(permitidas), conocidas.size(), LIMITE_GRATUITO,
                    candidatas.size(), candidatas.size() - permitidas.size(), fin);
        });
    }

    public void exigir(NegocioId negocioId, TipoContenidoPlan tipo, String clave, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            if (!seleccionar(negocioId, tipo, List.of(clave), ahora).autorizadas().contains(clave)) {
                throw new RestriccionPlanException("LIMITE_CONTENIDO_" + tipo.name(),
                        "Usaste los tres contenidos incluidos este mes. "
                                + "Con Premium podés acceder a todos desde Planes.");
            }
        });
    }
}
