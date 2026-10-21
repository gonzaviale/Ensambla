package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.PreparacionRenovacion;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RenovacionPremiumService {

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final PagoPremiumRepository pagoPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final PlanRepository planRepository;

    public Optional<RenovacionPremium> consultar(UUID negocioId) {
        return renovacionPremiumRepository.findUltimaByNegocioId(negocioId);
    }

    public RenovacionPremium obtener(UUID id) {
        return renovacionPremiumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
    }

    public Optional<RenovacionPremium> buscarPorProveedor(String proveedorId) {
        return renovacionPremiumRepository.findByProveedorId(proveedorId);
    }

    public List<UUID> listarParaSincronizar() {
        return renovacionPremiumRepository.listarParaSincronizar();
    }

    @Transactional
    public PreparacionRenovacion preparar(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Optional<RenovacionPremium> anterior = consultar(negocioId);
        if (anterior.isPresent() && !"CANCELADA".equals(anterior.get().getEstado())) {
            return new PreparacionRenovacion(anterior.get(), false);
        }
        Plan premium = obtenerPlan(CodigoPlan.PREMIUM);
        premium.validarLimitesConfigurados();
        if (!Boolean.TRUE.equals(premium.getConfigurado()) || premium.getPrecioMensual() == null
                || premium.getPrecioMensual().signum() <= 0 || !"ARS".equals(premium.getMoneda())) {
            throw new BusinessRuleException("El precio de Premium todavía no está configurado.");
        }
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        OffsetDateTime primerCobro = ahora;
        if (suscripcion.estaVigente(ahora) && premium.getId().equals(suscripcion.getPlanId())) {
            if (suscripcion.getFinEn() == null) {
                throw new BusinessRuleException("Tu negocio ya tiene Premium sin vencimiento.");
            }
            primerCobro = suscripcion.getFinEn();
        }
        // Mercado Pago conserva segundos: redondear hacia arriba evita anticipar el fin de la prueba.
        primerCobro = primerCobro.plusNanos(999_999_999).truncatedTo(ChronoUnit.SECONDS);
        RenovacionPremium renovacion = RenovacionPremium.builder()
                .id(UUID.randomUUID())
                .negocioId(negocioId)
                .estado("PREPARANDO")
                .importe(premium.getPrecioMensual())
                .moneda(premium.getMoneda())
                .primerCobroEn(primerCobro)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
        return new PreparacionRenovacion(renovacionPremiumRepository.save(renovacion), true);
    }

    @Transactional
    public RenovacionPremium solicitarCancelacion(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        RenovacionPremium renovacion = consultar(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
        renovacion.setCancelacionSolicitada(true);
        renovacion.setActualizadoEn(ahora);
        return renovacionPremiumRepository.save(renovacion);
    }

    @Transactional
    public RenovacionPremium actualizarContrato(UUID id, ContratoRemoto remoto, OffsetDateTime ahora) {
        bloquearRenovacion(id);
        RenovacionPremium renovacion = obtener(id);
        validarContrato(renovacion, remoto);
        if (renovacion.getModificadoProveedorEn() == null
                || !remoto.modificadoEn().isBefore(renovacion.getModificadoProveedorEn())) {
            renovacion.setProveedorId(remoto.id());
            renovacion.setEnlace(remoto.enlace());
            // CANCELADA es terminal: una lectura concurrente antigua no reabre el contrato.
            if (!"CANCELADA".equals(renovacion.getEstado())) {
                renovacion.setEstado(estadoLocal(remoto.estado()));
            }
            renovacion.setProximoCobroEn(remoto.proximoCobroEn());
            renovacion.setModificadoProveedorEn(remoto.modificadoEn());
        }
        renovacion.setSincronizadoEn(ahora);
        renovacion.setActualizadoEn(ahora);
        return renovacionPremiumRepository.save(renovacion);
    }

    @Transactional
    public void aplicarPago(UUID renovacionId, PagoRemoto remoto, OffsetDateTime ahora) {
        bloquearRenovacion(renovacionId);
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
    }

    @Transactional
    public void recalcular(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Optional<PagoPremium> vigente = pagoPremiumRepository.findVigente(negocioId, ahora);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        if (vigente.isPresent()) {
            PagoPremium pago = vigente.get();
            suscripcion.setPlanId(obtenerPlan(CodigoPlan.PREMIUM).getId());
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioEn(pago.getInicioEn());
            suscripcion.setFinEn(pago.getFinEn());
        } else if (suscripcion.getEstado() == EstadoSuscripcion.ACTIVA
                && suscripcion.getPlanId().equals(obtenerPlan(CodigoPlan.PREMIUM).getId())
                && pagoPremiumRepository.existeByNegocioId(negocioId)) {
            suscripcion.setPlanId(obtenerPlan(CodigoPlan.GRATUITO).getId());
            suscripcion.setInicioEn(ahora);
            suscripcion.setFinEn(null);
        } else {
            return;
        }
        suscripcion.setActualizadoEn(ahora);
        suscripcionRepository.save(suscripcion);
    }

    private void validarContrato(RenovacionPremium renovacion, ContratoRemoto remoto) {
        if (!renovacion.getId().equals(remoto.referencia()) || remoto.importe().compareTo(renovacion.getImporte()) != 0
                || !renovacion.getMoneda().equals(remoto.moneda()) || remoto.frecuencia() != 30
                || !"days".equals(remoto.unidad()) || !remoto.inicioEn().isEqual(renovacion.getPrimerCobroEn())
                || (renovacion.getProveedorId() != null && !renovacion.getProveedorId().equals(remoto.id()))) {
            throw new PasarelaPagoException();
        }
    }

    private String estadoLocal(String estado) {
        return switch (estado) {
            case "pending" -> "PENDIENTE";
            case "authorized" -> "AUTORIZADA";
            case "paused" -> "PAUSADA";
            case "cancelled", "canceled" -> "CANCELADA";
            default -> throw new PasarelaPagoException();
        };
    }

    private Plan obtenerPlan(CodigoPlan codigo) {
        return planRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    @Transactional
    public void marcarConsulta(UUID id, OffsetDateTime ahora) {
        bloquearRenovacion(id);
        RenovacionPremium renovacion = obtener(id);
        renovacion.setSincronizadoEn(ahora);
        renovacionPremiumRepository.save(renovacion);
    }

    private void bloquearRenovacion(UUID id) {
        UUID negocioId = renovacionPremiumRepository.findNegocioIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Suscripcion obtenerSuscripcion(UUID negocioId) {
        return suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }
}
