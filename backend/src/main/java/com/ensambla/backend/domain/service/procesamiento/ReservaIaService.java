package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/** La cuota mensual y la reserva diaria comparten transacción y bloqueo por negocio. */
@Service
@RequiredArgsConstructor
public class ReservaIaService {

    private final ControlPlanService controlPlanService;

    private final ProcesamientoRepository procesamientoRepository;

    @Transactional
    public boolean reservar(LoteTrabajo lote, int paginas, int limiteDiario, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(lote.negocioId(), paginas, ahora);
        return procesamientoRepository.reservar(lote, paginas, limiteDiario, ahora);
    }

    public void validarDisponibilidad(UUID negocioId, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(negocioId, 1, ahora);
    }
}
