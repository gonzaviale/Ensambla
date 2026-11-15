package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.AccesoIa;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ControlPlanService {

    private static final int LIMITE_GRATUITO = 6;

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final AccesoContenidoRepository accesoContenidoRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final NegocioRepository negocioRepository;

    private final AccesoIa accesoIa;

    /** Un negocio más de la cuenta tiene que entrar en el límite de su plan (Gratuito 1; Premium 6). */
    public void validarAltaNegocio(UsuarioId usuarioId, OffsetDateTime ahora) {
        conCuentaBloqueada(usuarioId, () -> exigirCupoDeNegocios(usuarioId, ahora));
    }

    /** Límite vigente de negocios de la cuenta; todo plan configurado debe tenerlo. */
    public int limiteNegocios(UsuarioId usuarioId, OffsetDateTime ahora) {
        return planVigenteDeLaCuenta(usuarioId, ahora).getLimiteNegocios();
    }

    public void validarAltaRegistros(NegocioId negocioId, long cantidad, OffsetDateTime ahora) {
        conNegocioBloqueado(negocioId, () -> exigirCupoDeRegistros(negocioId, cantidad, ahora));
    }

    /**
     * Con el acceso a la IA restringido, sólo el dueño de un email habilitado puede usarla (lectura, Sami y taller).
     *
     * @throws RestriccionPlanException si el dueño del negocio no está habilitado
     */
    public void exigirIa(NegocioId negocioId) {
        if (!estaHabilitadaLaIa(negocioId)) {
            throw new RestriccionPlanException("IA_NO_HABILITADA", AccesoIa.MENSAJE);
        }
    }

    public void validarPaginasIa(NegocioId negocioId, int cantidad, OffsetDateTime ahora) {
        exigirIa(negocioId);
        conNegocioBloqueado(negocioId, () -> exigirCupoDePaginasIa(negocioId, cantidad, ahora));
    }

    public void validarFuncion(NegocioId negocioId, FuncionPlan funcion, OffsetDateTime ahora) {
        planVigente(negocioId, ahora).validarFuncion(funcion);
    }

    /**
     * Admite un intento de lectura con IA: valida la cuota mensual o de la prueba, hace la {@code reserva} diaria y, si
     * entró, la descuenta de la prueba. Todo en una transacción con el negocio bloqueado.
     *
     * @return si la reserva diaria entró
     */
    public boolean reservarPaginasIa(NegocioId negocioId, int cantidad, OffsetDateTime ahora, BooleanSupplier reserva) {
        return transaccionPort.ejecutar(() -> admitirIntentoDeLectura(negocioId, cantidad, ahora, reserva));
    }

    /**
     * La reserva de una foto leída en el momento (sin pasar por el lote): además del cupo mensual del plan cuenta
     * contra el límite diario de fotos. Es la misma regla para la app y para WhatsApp.
     *
     * @throws RestriccionPlanException si se agotó el límite diario; el mensual lo informa {@link #validarPaginasIa}
     */
    public void reservarPaginaIaInmediata(NegocioId negocioId, int limiteDiario, OffsetDateTime ahora) {
        boolean reservada = reservarPaginasIa(negocioId, 1, ahora,
                () -> registrarPaginaInmediataDelDia(negocioId, limiteDiario, ahora));
        if (!reservada) {
            throw new RestriccionPlanException("LIMITE_IA_DIARIO", PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        }
    }

    public void exigirAccesoAlAviso(NegocioId negocioId, AvisoId avisoId, OffsetDateTime ahora) {
        exigir(negocioId, TipoContenidoPlan.AVISOS, avisoId.toString(), ahora);
    }

    public List<AvisoId> avisosAutorizados(NegocioId negocioId, List<AvisoId> avisos, OffsetDateTime ahora) {
        Set<String> autorizadas = clavesAutorizadasDe(negocioId, avisos, ahora);
        return avisos.stream().filter(aviso -> autorizadas.contains(aviso.toString())).toList();
    }

    public AccesoContenidoItem seleccionar(NegocioId negocioId, TipoContenidoPlan tipo, List<String> claves,
                                           OffsetDateTime ahora) {
        return conNegocioBloqueadoDevolviendo(negocioId, () -> seleccionarContenido(negocioId, tipo, claves, ahora));
    }

    public void exigir(NegocioId negocioId, TipoContenidoPlan tipo, String clave, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> exigirAccesoAlContenido(negocioId, tipo, clave, ahora));
    }

    private Set<String> clavesAutorizadasDe(NegocioId negocioId, List<AvisoId> avisos, OffsetDateTime ahora) {
        List<String> claves = avisos.stream().map(AvisoId::toString).toList();
        return Set.copyOf(seleccionar(negocioId, TipoContenidoPlan.AVISOS, claves, ahora).autorizadas());
    }

    private void conNegocioBloqueado(NegocioId negocioId, Runnable trabajo) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            trabajo.run();
        });
    }

    private <T> T conNegocioBloqueadoDevolviendo(NegocioId negocioId, Supplier<T> trabajo) {
        return transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            return trabajo.get();
        });
    }

    private void conCuentaBloqueada(UsuarioId usuarioId, Runnable trabajo) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearCuenta(usuarioId);
            trabajo.run();
        });
    }

    /** El plan, la prueba y el cupo de IA son de la cuenta: el negocio sólo dice de cuál. */
    private UsuarioId cuentaDe(NegocioId negocioId) {
        return negocioRepository.obtenerDuenioId(negocioId);
    }

    private void exigirCupoDeNegocios(UsuarioId usuarioId, OffsetDateTime ahora) {
        Plan plan = planVigenteDeLaCuenta(usuarioId, ahora);
        plan.validarNegocios(usoPlanRepository.contarNegocios(usuarioId));
    }

    private void exigirCupoDeRegistros(NegocioId negocioId, long cantidad, OffsetDateTime ahora) {
        Plan plan = planVigente(negocioId, ahora);
        plan.validarRegistros(usoPlanRepository.contarRegistros(negocioId), cantidad);
    }

    private boolean estaHabilitadaLaIa(NegocioId negocioId) {
        return !accesoIa.restringido() || accesoIa.permite(emailDelDuenio(negocioId));
    }

    private String emailDelDuenio(NegocioId negocioId) {
        return negocioRepository.findEmailDuenioById(negocioId).orElse(null);
    }

    private void exigirCupoDePaginasIa(NegocioId negocioId, int cantidad, OffsetDateTime ahora) {
        UsuarioId usuarioId = cuentaDe(negocioId);
        Plan plan = planVigenteDeLaCuenta(usuarioId, ahora);
        pruebaVigente(usuarioId, ahora).ifPresentOrElse(
                prueba -> prueba.validarPaginas(cantidad),
                () -> plan.validarPaginasIa(paginasUsadasEnElMes(usuarioId, ahora), cantidad));
    }

    private long paginasUsadasEnElMes(UsuarioId usuarioId, OffsetDateTime ahora) {
        LocalDate mes = ZonaNegocio.primerDiaDelMes(ahora);
        return usoPlanRepository.paginasEnPeriodo(usuarioId, mes, mes.plusMonths(1));
    }

    private boolean admitirIntentoDeLectura(
            NegocioId negocioId, int cantidad, OffsetDateTime ahora, BooleanSupplier reserva) {
        validarPaginasIa(negocioId, cantidad, ahora);
        boolean reservada = reserva.getAsBoolean();
        if (reservada) {
            registrarPaginasPrueba(negocioId, cantidad, ahora);
        }
        return reservada;
    }

    private boolean registrarPaginaInmediataDelDia(NegocioId negocioId, int limiteDiario, OffsetDateTime ahora) {
        return usoPlanRepository.registrarPaginaInmediata(negocioId, ZonaNegocio.dia(ahora), limiteDiario, ahora);
    }

    /** Se cobra al admitir un intento. También cuentan errores y timeouts, sin duplicar al finalizar el lote. */
    private void registrarPaginasPrueba(NegocioId negocioId, int cantidad, OffsetDateTime ahora) {
        conNegocioBloqueado(negocioId, () -> pruebaVigente(cuentaDe(negocioId), ahora)
                .ifPresent(prueba -> pruebaPremiumRepository.save(prueba.consumir(cantidad))));
    }

    private Optional<PruebaPremium> pruebaVigente(UsuarioId usuarioId, OffsetDateTime ahora) {
        if (!estaEnPruebaVigente(suscripcionDe(usuarioId), ahora)) {
            return Optional.empty();
        }
        return Optional.of(pruebaDe(usuarioId));
    }

    private boolean estaEnPruebaVigente(Suscripcion suscripcion, OffsetDateTime ahora) {
        return suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && suscripcion.estaVigente(ahora);
    }

    private Plan planVigente(NegocioId negocioId, OffsetDateTime ahora) {
        return planVigenteDeLaCuenta(cuentaDe(negocioId), ahora);
    }

    private Plan planVigenteDeLaCuenta(UsuarioId usuarioId, OffsetDateTime ahora) {
        Suscripcion suscripcion = suscripcionDe(usuarioId);
        Plan plan = suscripcion.estaVigente(ahora) ? planContratadoPor(suscripcion) : planGratuito();
        plan.validarLimitesConfigurados();
        return plan;
    }

    private boolean esPremium(NegocioId negocioId, OffsetDateTime ahora) {
        return planVigente(negocioId, ahora).getCodigo() == CodigoPlan.PREMIUM;
    }

    private Suscripcion suscripcionDe(UsuarioId usuarioId) {
        return suscripcionRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }

    private PruebaPremium pruebaDe(UsuarioId usuarioId) {
        return pruebaPremiumRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Prueba no encontrada"));
    }

    private Plan planContratadoPor(Suscripcion suscripcion) {
        return planRepository.findById(suscripcion.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    private Plan planGratuito() {
        return planRepository.findByCodigo(CodigoPlan.GRATUITO)
                .orElseThrow(() -> new ResourceNotFoundException("Plan gratuito no encontrado"));
    }

    private AccesoContenidoItem seleccionarContenido(
            NegocioId negocioId, TipoContenidoPlan tipo, List<String> claves, OffsetDateTime ahora) {
        LocalDate periodo = ZonaNegocio.primerDiaDelMes(ahora);
        OffsetDateTime finDelPeriodo = ZonaNegocio.inicioDelDia(periodo.plusMonths(1));
        List<String> candidatas = claves.stream().distinct().toList();
        if (esPremium(negocioId, ahora)) {
            return accesoSinLimite(candidatas, finDelPeriodo);
        }
        return accesoConLimiteGratuito(negocioId, tipo, periodo, candidatas, finDelPeriodo, ahora);
    }

    private AccesoContenidoItem accesoSinLimite(List<String> candidatas, OffsetDateTime finDelPeriodo) {
        return new AccesoContenidoItem(candidatas, 0, null, candidatas.size(), 0, finDelPeriodo);
    }

    private AccesoContenidoItem accesoConLimiteGratuito(NegocioId negocioId, TipoContenidoPlan tipo, LocalDate periodo,
            List<String> candidatas, OffsetDateTime finDelPeriodo, OffsetDateTime ahora) {
        Set<String> conocidas = accesoContenidoRepository.autorizadas(negocioId, tipo, periodo);
        List<String> permitidas = new ArrayList<>();
        for (String clave : candidatas) {
            Runnable registro = () -> accesoContenidoRepository.registrar(negocioId, tipo, periodo, clave, ahora);
            if (autorizarSiHayCupo(clave, conocidas, registro)) {
                permitidas.add(clave);
            }
        }
        return new AccesoContenidoItem(List.copyOf(permitidas), conocidas.size(), LIMITE_GRATUITO,
                candidatas.size(), candidatas.size() - permitidas.size(), finDelPeriodo);
    }

    private boolean autorizarSiHayCupo(String clave, Set<String> conocidas, Runnable registro) {
        if (conocidas.contains(clave)) {
            return true;
        }
        if (conocidas.size() >= LIMITE_GRATUITO) {
            return false;
        }
        registro.run();
        conocidas.add(clave);
        return true;
    }

    private void exigirAccesoAlContenido(
            NegocioId negocioId, TipoContenidoPlan tipo, String clave, OffsetDateTime ahora) {
        boolean autorizado = seleccionar(negocioId, tipo, List.of(clave), ahora).autorizadas().contains(clave);
        if (!autorizado) {
            throw new RestriccionPlanException("LIMITE_CONTENIDO_" + tipo.name(),
                    "Usaste los " + LIMITE_GRATUITO + " contenidos incluidos este mes. "
                            + "Con Premium podés acceder a todos desde Planes.");
        }
    }
}
