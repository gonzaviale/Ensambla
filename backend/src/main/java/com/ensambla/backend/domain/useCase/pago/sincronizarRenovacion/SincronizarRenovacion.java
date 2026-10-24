package com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class SincronizarRenovacion {

    private final RenovacionPremiumService renovacionPremiumService;

    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    private final PagoPremiumRepository pagoPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final SuscripcionService suscripcionService;

    private final SuscripcionRepository suscripcionRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute(UUID id) {
        RenovacionPremium renovacion = renovacionPremiumService.obtener(id);
        Optional<ContratoRemoto> remoto = renovacion.getProveedorId() == null
                ? pasarelaSuscripcionesPort.buscar(renovacion.getId())
                : Optional.of(pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId()));
        if (remoto.isEmpty()) {
            return;
        }
        renovacion = renovacionPremiumService.actualizarContrato(id, remoto.get(), OffsetDateTime.now(clock));
        if (renovacion.isCancelacionSolicitada() && !"CANCELADA".equals(renovacion.getEstado())) {
            pasarelaSuscripcionesPort.cancelar(renovacion.getProveedorId());
            ContratoRemoto cancelado = pasarelaSuscripcionesPort.consultar(renovacion.getProveedorId());
            renovacion = renovacionPremiumService.actualizarContrato(id, cancelado, OffsetDateTime.now(clock));
        }
        for (PagoRemoto pago : pasarelaSuscripcionesPort.listarPagos(renovacion.getProveedorId())) {
            aplicarPago(id, pago, OffsetDateTime.now(clock));
        }
        recalcular(renovacion.getNegocioId(), OffsetDateTime.now(clock));
    }

    private void aplicarPago(UUID renovacionId, PagoRemoto remoto, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            renovacionPremiumService.bloquear(renovacionId);
            RenovacionPremium renovacion = renovacionPremiumService.obtener(renovacionId);
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
                    .negocioId(renovacion.getNegocioId())
                    .renovacionId(renovacionId)
                    .pagoId(remoto.pagoId())
                    .estado(remoto.estado())
                    .inicioEn(remoto.debitoEn())
                    .finEn(remoto.debitoEn().plusHours(720))
                    .modificadoProveedorEn(remoto.modificadoEn())
                    .actualizadoEn(ahora)
                    .build());
            recalcular(renovacion.getNegocioId(), ahora);
        });
    }

    /** Premium mientras haya un pago vigente; cuando ya no queda ninguno, el negocio vuelve al plan gratuito. */
    private void recalcular(UUID negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Optional<PagoPremium> vigente = pagoPremiumRepository.findVigente(negocioId, ahora);
            Suscripcion suscripcion = suscripcionService.obtener(negocioId);
            if (vigente.isPresent()) {
                PagoPremium pago = vigente.get();
                suscripcion.setPlanId(suscripcionService.obtenerPlan(CodigoPlan.PREMIUM).getId());
                suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
                suscripcion.setInicioEn(pago.getInicioEn());
                suscripcion.setFinEn(pago.getFinEn());
            } else if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                    && suscripcion.getPlanId().equals(suscripcionService.obtenerPlan(CodigoPlan.PREMIUM).getId())
                    && pagoPremiumRepository.existeByNegocioId(negocioId)) {
                suscripcion.setPlanId(suscripcionService.obtenerPlan(CodigoPlan.GRATUITO).getId());
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
