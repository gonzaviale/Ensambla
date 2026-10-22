package com.ensambla.backend.domain.service.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
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

    public List<Sistema> listar(UUID negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId);
    }

    /** Crea el sistema o lo reemplaza; si el id es de un proyecto de otro negocio responde como si no existiera. */
    @Transactional
    public Sistema guardar(UUID negocioId, Sistema sistema) {
        sistemaRepository.findNegocioIdByProyectoId(sistema.id())
                .filter(duenio -> !duenio.equals(negocioId))
                .ifPresent(duenio -> {
                    throw new ResourceNotFoundException("Sistema no encontrado");
                });
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
