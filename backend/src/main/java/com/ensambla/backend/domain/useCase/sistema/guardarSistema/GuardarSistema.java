package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class GuardarSistema {

    private final NegocioService negocioService;

    private final SistemaRepository sistemaRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public SistemaItem execute(GuardarSistemaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Sistema sistema = request.sistema().aSistema(negocioId, request.id(), OffsetDateTime.now(clock));
        return SistemaItem.de(transaccionPort.ejecutar(() -> guardar(negocioId, sistema)));
    }

    /** Crea el sistema o lo reemplaza; si el id es de un proyecto de otro negocio responde como si no existiera. */
    private Sistema guardar(UUID negocioId, Sistema sistema) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Optional<UUID> duenioActual = sistemaRepository.findNegocioIdByProyectoId(sistema.id());
        validarDuenio(negocioId, duenioActual);
        if (duenioActual.isEmpty()) {
            controlPlanService.validarAltaProyecto(negocioId, sistema.proyecto().getActualizadoEn());
        }
        validarRegistrosNuevos(negocioId, sistema);
        sistemaRepository.guardar(sistema);
        return sistema;
    }

    private void validarDuenio(UUID negocioId, Optional<UUID> duenioActual) {
        duenioActual
                .filter(duenio -> !duenio.equals(negocioId))
                .ifPresent(duenio -> {
                    throw new ResourceNotFoundException("Sistema no encontrado");
                });
    }

    /** Sólo cuenta contra el plan lo que el reemplazo suma respecto de lo que ya había. */
    private void validarRegistrosNuevos(UUID negocioId, Sistema sistema) {
        long cantidad = sistema.registrosPorBloque().values().stream().mapToLong(List::size).sum();
        long anteriores = usoPlanRepository.contarRegistrosProyecto(negocioId, sistema.id());
        if (cantidad > anteriores) {
            controlPlanService.validarAltaRegistros(
                    negocioId, cantidad - anteriores, sistema.proyecto().getActualizadoEn());
        }
    }
}
