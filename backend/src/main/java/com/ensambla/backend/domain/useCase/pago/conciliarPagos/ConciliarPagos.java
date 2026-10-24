package com.ensambla.backend.domain.useCase.pago.conciliarPagos;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.OffsetDateTime;

@Slf4j
@RequiredArgsConstructor
public class ConciliarPagos {

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacion sincronizarRenovacion;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute() {
        for (RenovacionId id : renovacionPremiumRepository.listarParaSincronizar()) {
            try {
                sincronizarRenovacion.execute(id);
            } catch (PasarelaPagoException exception) {
                log.warn("Conciliación de renovación pendiente: {}", id);
            } finally {
                marcarConsulta(id, OffsetDateTime.now(clock));
            }
        }
    }

    /** Aunque la sincronización falle, la renovación queda consultada y no se reintenta enseguida. */
    private void marcarConsulta(RenovacionId id, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            renovacionPremiumService.bloquear(id);
            RenovacionPremium renovacion = renovacionPremiumService.obtener(id);
            renovacion.setSincronizadoEn(ahora);
            renovacionPremiumRepository.save(renovacion);
        });
    }
}
