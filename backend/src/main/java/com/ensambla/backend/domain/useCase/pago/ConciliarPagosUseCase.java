package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ConciliarPagosUseCase {

    private final RenovacionPremiumService renovacionPremiumService;

    private final SincronizarRenovacionUseCase sincronizarRenovacionUseCase;

    private final Clock clock;

    public void execute() {
        for (UUID id : renovacionPremiumService.listarParaSincronizar()) {
            try {
                sincronizarRenovacionUseCase.execute(id);
            } catch (PasarelaPagoException exception) {
                log.warn("Conciliación de renovación pendiente: {}", id);
            } finally {
                renovacionPremiumService.marcarConsulta(id, OffsetDateTime.now(clock));
            }
        }
    }
}
