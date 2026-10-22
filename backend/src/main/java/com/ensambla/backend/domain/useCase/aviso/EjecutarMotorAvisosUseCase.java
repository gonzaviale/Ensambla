package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.service.aviso.MotorAvisosService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Corre el motor de monitoreo sobre todos los negocios; un negocio con error no frena a los demás. */
@Slf4j
@RequiredArgsConstructor
public class EjecutarMotorAvisosUseCase {

    private final NegocioService negocioService;
    private final MotorAvisosService motorAvisosService;
    private final Clock clock;

    public void execute() {
        for (UUID negocioId : negocioService.listarIds()) {
            ejecutarNegocio(negocioId);
        }
    }

    private void ejecutarNegocio(UUID negocioId) {
        try {
            motorAvisosService.ejecutar(negocioId, OffsetDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("El motor de avisos falló en el negocio {}", negocioId, e);
        }
    }
}
