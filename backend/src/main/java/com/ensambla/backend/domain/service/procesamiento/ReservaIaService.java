package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Las cuotas mensual/de prueba y la reserva diaria comparten transacción y bloqueo por negocio. */
@RequiredArgsConstructor
public class ReservaIaService {

    private final ControlPlanService controlPlanService;

    private final ProcesamientoRepository procesamientoRepository;

    private final TransaccionPort transaccionPort;

    public boolean reservar(LoteTrabajo lote, int paginas, int limiteDiario, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            controlPlanService.validarPaginasIa(lote.negocioId(), paginas, ahora);
            boolean reservada = procesamientoRepository.reservar(lote, paginas, limiteDiario, ahora);
            if (reservada) {
                controlPlanService.registrarPaginasPrueba(lote.negocioId(), paginas, ahora);
            }
            return reservada;
        });
    }

    public void validarDisponibilidad(UUID negocioId, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(negocioId, 1, ahora);
    }
}
