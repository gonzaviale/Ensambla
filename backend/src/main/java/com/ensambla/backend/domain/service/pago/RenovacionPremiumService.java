package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
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
        return transaccionPort.ejecutar(() -> {
            bloquear(id);
            RenovacionPremium renovacion = obtener(id);
            renovacion.validarContrato(remoto);
            if (renovacion.getModificadoProveedorEn() == null
                    || !remoto.modificadoEn().isBefore(renovacion.getModificadoProveedorEn())) {
                renovacion.setProveedorId(remoto.id());
                renovacion.setEnlace(remoto.enlace());
                // CANCELADA es terminal: una lectura concurrente antigua no reabre el contrato.
                if (!"CANCELADA".equals(renovacion.getEstado())) {
                    renovacion.setEstado(RenovacionPremium.estadoLocal(remoto.estado()));
                }
                renovacion.setProximoCobroEn(remoto.proximoCobroEn());
                renovacion.setModificadoProveedorEn(remoto.modificadoEn());
            }
            renovacion.setSincronizadoEn(ahora);
            renovacion.setActualizadoEn(ahora);
            return renovacionPremiumRepository.save(renovacion);
        });
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
        RenovacionPremium renovacion = obtener(id);
        Optional<ContratoRemoto> remoto = renovacion.getProveedorId() == null
                ? pasarelaSuscripcionesPort.buscar(renovacion.getId())
                : Optional.of(pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId()));
        if (remoto.isEmpty()) {
            return;
        }
        renovacion = actualizarContrato(id, remoto.get(), ahora);
        if (renovacion.isCancelacionSolicitada() && !"CANCELADA".equals(renovacion.getEstado())) {
            pasarelaSuscripcionesPort.cancelar(renovacion.getProveedorId());
            ContratoRemoto cancelado = pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId());
            renovacion = actualizarContrato(id, cancelado, ahora);
        }
        for (PagoRemoto pago : pasarelaSuscripcionesPort.listarPagos(renovacion.getProveedorId())) {
            aplicarPago(id, pago, ahora);
        }
        recalcular(renovacion.getUsuarioId(), ahora);
    }

    private void aplicarPago(RenovacionId renovacionId, PagoRemoto remoto, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            bloquear(renovacionId);
            RenovacionPremium renovacion = obtener(renovacionId);
            if (!renovacion.getId().equals(remoto.referencia())
                    || !remoto.contratoId().equals(renovacion.getProveedorId())
                    || remoto.importe().compareTo(renovacion.getImporte()) != 0
                    || !renovacion.getMoneda().equals(remoto.moneda())
                    || remoto.debitoEn().isBefore(renovacion.getPrimerCobroEn())) {
                throw new PasarelaPagoException();
            }
            Optional<PagoPremium> anterior = pagoPremiumRepository.findByFacturaId(remoto.facturaId());
            if (anterior.isPresent()) {
                PagoPremium pago = anterior.get();
                if (!pago.getRenovacionId().equals(renovacionId) || !pago.getInicioEn().isEqual(remoto.debitoEn())) {
                    throw new PasarelaPagoException();
                }
                if (!remoto.modificadoEn().isAfter(pago.getModificadoProveedorEn())) {
                    return;
                }
            }
            pagoPremiumRepository.save(PagoPremium.builder()
                    .facturaId(remoto.facturaId())
                    .usuarioId(renovacion.getUsuarioId())
                    .renovacionId(renovacionId)
                    .pagoId(remoto.pagoId())
                    .estado(remoto.estado())
                    .inicioEn(remoto.debitoEn())
                    .finEn(remoto.debitoEn().plusHours(720))
                    .modificadoProveedorEn(remoto.modificadoEn())
                    .actualizadoEn(ahora)
                    .build());
            recalcular(renovacion.getUsuarioId(), ahora);
        });
    }

    /** Premium mientras haya un pago vigente; cuando ya no queda ninguno, la cuenta vuelve al plan gratuito. */
    private void recalcular(UsuarioId usuarioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearCuenta(usuarioId);
            Optional<PagoPremium> vigente = pagoPremiumRepository.findVigente(usuarioId, ahora);
            Suscripcion suscripcion = suscripcionRepository.obtener(usuarioId);
            if (vigente.isPresent()) {
                PagoPremium pago = vigente.get();
                suscripcion.setPlanId(planRepository.obtener(CodigoPlan.PREMIUM).getId());
                suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
                suscripcion.setInicioEn(pago.getInicioEn());
                suscripcion.setFinEn(pago.getFinEn());
            } else if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                    && suscripcion.getPlanId().equals(planRepository.obtener(CodigoPlan.PREMIUM).getId())
                    && pagoPremiumRepository.existeByUsuarioId(usuarioId)) {
                suscripcion.setPlanId(planRepository.obtener(CodigoPlan.GRATUITO).getId());
                suscripcion.setInicioEn(ahora);
                suscripcion.setFinEn(null);
            } else {
                return;
            }
            suscripcion.setActualizadoEn(ahora);
            suscripcionRepository.save(suscripcion);
        });
    }
}
