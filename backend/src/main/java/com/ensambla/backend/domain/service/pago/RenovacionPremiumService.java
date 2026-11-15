package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class RenovacionPremiumService {

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final PagoPremiumRepository pagoPremiumRepository;

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final TransaccionPort transaccionPort;

    public Optional<RenovacionPremium> consultar(UsuarioId usuarioId) {
        return renovacionPremiumRepository.findUltimaByUsuarioId(usuarioId);
    }

    public RenovacionPremium obtener(RenovacionId id) {
        return renovacionPremiumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
    }

    public RenovacionPremium actualizarContrato(RenovacionId id, ContratoRemoto remoto, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> actualizarContratoBloqueando(id, remoto, ahora));
    }

    private RenovacionPremium actualizarContratoBloqueando(
            RenovacionId id, ContratoRemoto remoto, OffsetDateTime ahora) {
        bloquear(id);
        RenovacionPremium renovacion = obtener(id);
        renovacion.actualizarDesde(remoto, ahora);
        return guardar(renovacion);
    }

    private RenovacionPremium guardar(RenovacionPremium renovacion) {
        return renovacionPremiumRepository.save(renovacion);
    }

    /** Bloquea la cuenta dueña de la renovación; se llama dentro de la transacción que la modifica. */
    public void bloquear(RenovacionId id) {
        UsuarioId usuarioId = renovacionPremiumRepository.findUsuarioIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
        usoPlanRepository.bloquearCuenta(usuarioId);
    }

    /**
     * Trae del proveedor el estado del contrato y de sus pagos y deja a la cuenta con el plan que corresponde: Premium
     * mientras haya un pago vigente y el gratuito cuando ya no queda ninguno.
     */
    public void sincronizar(RenovacionId id, OffsetDateTime ahora) {
        buscarContratoDelProveedor(obtener(id))
                .ifPresent(contrato -> sincronizarContratoYPagos(id, contrato, ahora));
    }

    private Optional<ContratoRemoto> buscarContratoDelProveedor(RenovacionPremium renovacion) {
        if (renovacion.tieneContratoEnElProveedor()) {
            return Optional.of(pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId()));
        }
        return pasarelaSuscripcionesPort.buscar(renovacion.getId());
    }

    private void sincronizarContratoYPagos(RenovacionId id, ContratoRemoto contrato, OffsetDateTime ahora) {
        RenovacionPremium renovacion = actualizarContrato(id, contrato, ahora);
        renovacion = cancelarEnElProveedorSiSeSolicito(id, renovacion, ahora);
        aplicarPagosDelProveedor(id, renovacion.getProveedorId(), ahora);
        recalcular(renovacion.getUsuarioId(), ahora);
    }

    private RenovacionPremium cancelarEnElProveedorSiSeSolicito(
            RenovacionId id, RenovacionPremium renovacion, OffsetDateTime ahora) {
        if (!renovacion.debeCancelarseEnElProveedor()) {
            return renovacion;
        }
        pasarelaSuscripcionesPort.cancelar(renovacion.getProveedorId());
        ContratoRemoto cancelado = pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId());
        return actualizarContrato(id, cancelado, ahora);
    }

    private void aplicarPagosDelProveedor(RenovacionId id, String contratoId, OffsetDateTime ahora) {
        pasarelaSuscripcionesPort.listarPagos(contratoId).forEach(pago -> aplicarPago(id, pago, ahora));
    }

    private void aplicarPago(RenovacionId renovacionId, PagoRemoto remoto, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            bloquear(renovacionId);
            RenovacionPremium renovacion = obtener(renovacionId);
            renovacion.validarPago(remoto);
            if (yaEstaRegistrado(renovacionId, remoto)) {
                return;
            }
            pagoPremiumRepository.save(PagoPremium.de(remoto, renovacion, ahora));
            recalcular(renovacion.getUsuarioId(), ahora);
        });
    }

    private boolean yaEstaRegistrado(RenovacionId renovacionId, PagoRemoto remoto) {
        Optional<PagoPremium> registrado = pagoPremiumRepository.findByFacturaId(remoto.facturaId());
        registrado.ifPresent(pago -> pago.validarMismoPago(renovacionId, remoto));
        return registrado.filter(pago -> pago.estaAlDiaCon(remoto)).isPresent();
    }

    /** Premium mientras haya un pago vigente; cuando ya no queda ninguno, la cuenta vuelve al plan gratuito. */
    private void recalcular(UsuarioId usuarioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearCuenta(usuarioId);
            Optional<PagoPremium> vigente = pagoPremiumRepository.findVigente(usuarioId, ahora);
            Suscripcion suscripcion = suscripcionRepository.obtener(usuarioId);
            vigente.ifPresentOrElse(
                    pago -> pasarAPremium(suscripcion, pago, ahora),
                    () -> volverAlPlanGratuitoSiCorresponde(usuarioId, suscripcion, ahora));
        });
    }

    private void pasarAPremium(Suscripcion suscripcion, PagoPremium pago, OffsetDateTime ahora) {
        suscripcion.activarPlan(planId(CodigoPlan.PREMIUM), pago.getInicioEn(), pago.getFinEn(), ahora);
        suscripcionRepository.save(suscripcion);
    }

    private void volverAlPlanGratuitoSiCorresponde(
            UsuarioId usuarioId, Suscripcion suscripcion, OffsetDateTime ahora) {
        if (!estabaEnPremiumPorPagos(usuarioId, suscripcion)) {
            return;
        }
        suscripcion.activarPlan(planId(CodigoPlan.GRATUITO), ahora, null, ahora);
        suscripcionRepository.save(suscripcion);
    }

    private boolean estabaEnPremiumPorPagos(UsuarioId usuarioId, Suscripcion suscripcion) {
        return suscripcion.estaActiva()
                && suscripcion.esDelPlan(planId(CodigoPlan.PREMIUM))
                && pagoPremiumRepository.existeByUsuarioId(usuarioId);
    }

    private PlanId planId(CodigoPlan codigo) {
        return planRepository.obtener(codigo).getId();
    }
}
