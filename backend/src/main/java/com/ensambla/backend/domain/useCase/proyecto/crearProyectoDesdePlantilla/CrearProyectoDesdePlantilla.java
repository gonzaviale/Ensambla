package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CrearProyectoDesdePlantilla {

    private final NegocioRepository negocioRepository;

    private final PlantillaRepository plantillaRepository;

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdePlantillaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = obtenerPlantilla(request.plantillaId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Proyecto proyecto = crearEnTransaccion(negocioId, plantilla, ahora);
        return ProyectoItem.de(proyecto);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private PlantillaProyecto obtenerPlantilla(PlantillaId plantillaId) {
        return plantillaRepository.obtenerParaProyecto(plantillaId);
    }

    private Proyecto crearEnTransaccion(NegocioId negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> crear(negocioId, plantilla, ahora));
    }

    /** Guarda toda la estructura de la plantilla en la misma operación que el control del cupo. */
    private Proyecto crear(NegocioId negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        validarCupoDeProyectos(negocioId, ahora);
        return guardarConEstructura(Proyecto.desdePlantilla(negocioId, plantilla, ahora));
    }

    private void validarCupoDeProyectos(NegocioId negocioId, OffsetDateTime ahora) {
        controlPlanService.validarAltaProyecto(negocioId, ahora);
    }

    private Proyecto guardarConEstructura(Proyecto proyecto) {
        return proyectoRepository.saveConEstructura(proyecto);
    }
}
