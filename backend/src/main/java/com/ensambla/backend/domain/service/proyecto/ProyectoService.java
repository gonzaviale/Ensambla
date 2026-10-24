package com.ensambla.backend.domain.service.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    public List<Proyecto> listar(UUID negocioId) {
        return proyectoRepository.findAllByNegocioId(negocioId);
    }

    public Proyecto obtener(UUID negocioId, UUID proyectoId) {
        return proyectoRepository.findByIdAndNegocioId(proyectoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));
    }

    public Proyecto crear(UUID negocioId, String nombre, String descripcion, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            controlPlanService.validarAltaProyecto(negocioId, ahora);
            return proyectoRepository.save(Proyecto.enBlanco(negocioId, nombre, descripcion, ahora));
        });
    }

    public Proyecto crearDesdePlantilla(UUID negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            controlPlanService.validarAltaProyecto(negocioId, ahora);
            return proyectoRepository.saveConEstructura(Proyecto.desdePlantilla(negocioId, plantilla, ahora));
        });
    }

    /**
     * Copia la plantilla y le suma las columnas extra, todo en una transacción: si una columna es inválida no
     * queda guardado nada. {@code nombre} es opcional; sin él, el proyecto se llama como la plantilla.
     */
    public Proyecto crearDesdePlantilla(UUID negocioId, PlantillaProyecto plantilla, String nombre,
                                        List<ConfiguracionCampo> columnasExtra, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            controlPlanService.validarAltaProyecto(negocioId, ahora);
            Proyecto proyecto = Proyecto.desdePlantilla(negocioId, plantilla, ahora);
            proyecto.agregarColumnas(columnasExtra, ahora);
            if (nombre != null && !nombre.isBlank()) {
                proyecto.renombrar(nombre, ahora);
            }
            return proyectoRepository.saveConEstructura(proyecto);
        });
    }

    public Proyecto renombrar(UUID negocioId, UUID proyectoId, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            Proyecto proyecto = obtener(negocioId, proyectoId);
            proyecto.renombrar(nombre, ahora);
            return proyectoRepository.save(proyecto);
        });
    }

    public void eliminar(UUID negocioId, UUID proyectoId) {
        transaccionPort.ejecutar(() -> {
            obtener(negocioId, proyectoId);
            proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
        });
    }
}

