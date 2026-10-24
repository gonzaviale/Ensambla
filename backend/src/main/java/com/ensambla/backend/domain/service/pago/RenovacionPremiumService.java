package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class RenovacionPremiumService {

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final TransaccionPort transaccionPort;

    public Optional<RenovacionPremium> consultar(UUID negocioId) {
        return renovacionPremiumRepository.findUltimaByNegocioId(negocioId);
    }

    public RenovacionPremium obtener(UUID id) {
        return renovacionPremiumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
    }

    public RenovacionPremium actualizarContrato(UUID id, ContratoRemoto remoto, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            bloquear(id);
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
        });
    }

    /** Bloquea el negocio dueño de la renovación; se llama dentro de la transacción que la modifica. */
    public void bloquear(UUID id) {
        UUID negocioId = renovacionPremiumRepository.findNegocioIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renovación no encontrada"));
        usoPlanRepository.bloquearNegocio(negocioId);
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
}
