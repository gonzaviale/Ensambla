package com.ensambla.backend.domain.service.reposicion;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

/** Lo que repiten los casos de uso de reposición: traer el sistema del negocio y ver qué productos hay que reponer. */
@RequiredArgsConstructor
public class ReposicionService {

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    /** @throws com.ensambla.backend.domain.exception.ResourceNotFoundException si el sistema es de otro negocio */
    public Faltantes faltantesDe(NegocioId negocioId, ProyectoId proyectoId) {
        Proyecto proyecto = proyectoRepository.obtener(negocioId, proyectoId);
        return Faltantes.de(datosDeProyectoService.cargar(proyecto, negocioId));
    }
}
