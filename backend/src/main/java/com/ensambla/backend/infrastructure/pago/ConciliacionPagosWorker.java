package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ensambla.pagos.mercadopago", name = "habilitado", havingValue = "true")
public class ConciliacionPagosWorker {

    private final ConciliarPagos conciliarPagos;

    @Scheduled(fixedDelayString = "${ensambla.pagos.intervalo:60000}",
            initialDelayString = "${ensambla.pagos.demora-inicial:10000}")
    public void conciliar() {
        conciliarPagos.execute();
    }
}
