package com.ensambla.backend.domain.service.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SistemaService {

    private final SistemaRepository sistemaRepository;

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    public List<Sistema> listar(UUID negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId);
    }

    /** Crea el sistema o lo reemplaza; si el id es de un proyecto de otro negocio responde como si no existiera. */
    @Transactional
    public Sistema guardar(UUID negocioId, Sistema sistema) {
        usoPlanRepository.bloquearNegocio(negocioId);
        var duenioActual = sistemaRepository.findNegocioIdByProyectoId(sistema.id());
        duenioActual
                .filter(duenio -> !duenio.equals(negocioId))
                .ifPresent(duenio -> {
                    throw new ResourceNotFoundException("Sistema no encontrado");
                });
        if (duenioActual.isEmpty()) {
            controlPlanService.validarAltaProyecto(negocioId, sistema.proyecto().getActualizadoEn());
        }
        long cantidad = sistema.registrosPorBloque().values().stream().mapToLong(List::size).sum();
        long anteriores = usoPlanRepository.contarRegistrosProyecto(negocioId, sistema.id());
        if (cantidad > anteriores) {
            controlPlanService.validarAltaRegistros(
                    negocioId, cantidad - anteriores, sistema.proyecto().getActualizadoEn());
        }
        sistemaRepository.guardar(sistema);
        return sistema;
    }

    /**
     * Elimina el sistema y todo lo que contiene. Es idempotente: si no existe, o es de otro negocio, no hace nada,
     * así la pantalla puede reintentar un borrado sin que falle.
     */
    @Transactional
    public void eliminar(UUID negocioId, UUID proyectoId) {
        proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }
}
