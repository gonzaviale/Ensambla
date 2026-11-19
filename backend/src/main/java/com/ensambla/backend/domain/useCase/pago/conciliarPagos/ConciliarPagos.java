package com.ensambla.backend.domain.useCase.pago.conciliarPagos;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ConciliarPagos {

    private final RenovacionPremiumService renovacionPremiumService;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute() {
        for (RenovacionId id : listarParaSincronizar()) {
            sincronizarYMarcarConsulta(id);
        }
    }

    private void sincronizarYMarcarConsulta(RenovacionId id) {
        try {
            sincronizar(id);
        } catch (PasarelaPagoException exception) {
            log.warn("Conciliación de renovación pendiente: {}", id);
        } finally {
            marcarConsulta(id, OffsetDateTime.now(clock));
        }
    }

    /** Aunque la sincronización falle, la renovación queda consultada y no se reintenta enseguida. */
    private void marcarConsulta(RenovacionId id, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> bloquearYMarcarConsulta(id, ahora));
    }

    private void bloquearYMarcarConsulta(RenovacionId id, OffsetDateTime ahora) {
        renovacionPremiumService.bloquear(id);
        RenovacionPremium renovacion = renovacionPremiumService.obtener(id);
        renovacion.registrarConsulta(ahora);
        guardar(renovacion);
    }

    private List<RenovacionId> listarParaSincronizar() {
        return renovacionPremiumRepository.listarParaSincronizar();
    }

    private void sincronizar(RenovacionId id) {
        renovacionPremiumService.sincronizar(id, OffsetDateTime.now(clock));
    }

    private void guardar(RenovacionPremium renovacion) {
        renovacionPremiumRepository.save(renovacion);
    }
}
