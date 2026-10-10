package com.ensambla.backend.domain.service.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;

    public List<Proyecto> listar(UUID negocioId) {
        return proyectoRepository.findAllByNegocioId(negocioId);
    }

    public Proyecto obtener(UUID negocioId, UUID proyectoId) {
        return proyectoRepository.findByIdAndNegocioId(proyectoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));
    }

    @Transactional
    public Proyecto crear(UUID negocioId, String nombre, String descripcion, OffsetDateTime ahora) {
        return proyectoRepository.save(Proyecto.enBlanco(negocioId, nombre, descripcion, ahora));
    }

    @Transactional
    public Proyecto crearDesdePlantilla(UUID negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        return proyectoRepository.saveConEstructura(Proyecto.desdePlantilla(negocioId, plantilla, ahora));
    }

    @Transactional
    public Proyecto renombrar(UUID negocioId, UUID proyectoId, String nombre, OffsetDateTime ahora) {
        Proyecto proyecto = obtener(negocioId, proyectoId);
        proyecto.renombrar(nombre, ahora);
        return proyectoRepository.save(proyecto);
    }

    @Transactional
    public void eliminar(UUID negocioId, UUID proyectoId) {
        obtener(negocioId, proyectoId);
        proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }
}

