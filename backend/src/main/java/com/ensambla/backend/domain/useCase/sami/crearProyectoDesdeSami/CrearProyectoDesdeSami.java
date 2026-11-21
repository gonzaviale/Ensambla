package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
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
public class CrearProyectoDesdeSami {

    private final NegocioRepository negocioRepository;

    private final PlantillaRepository plantillaRepository;

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoDesdeSamiRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        PlantillaProyecto plantilla = obtenerPlantilla(request.plantillaId());
        Proyecto proyecto = crearEnTransaccion(negocioId, plantilla, request, OffsetDateTime.now(clock));
        return ProyectoItem.de(proyecto);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private PlantillaProyecto obtenerPlantilla(PlantillaId plantillaId) {
        return plantillaRepository.obtenerParaProyecto(plantillaId);
    }

    private Proyecto crearEnTransaccion(NegocioId negocioId, PlantillaProyecto plantilla,
                                        CrearProyectoDesdeSamiRequest request, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> crear(negocioId, plantilla, request, ahora));
    }

    /**
     * Copia la plantilla y le suma las columnas extra, todo en una transacción: si una columna es inválida no queda
     * guardado nada. El nombre es opcional; sin él, el proyecto se llama como la plantilla.
     */
    private Proyecto crear(NegocioId negocioId, PlantillaProyecto plantilla, CrearProyectoDesdeSamiRequest request,
                           OffsetDateTime ahora) {
        controlPlanService.validarAltaProyecto(negocioId, ahora);
        Proyecto proyecto = Proyecto.desdePlantilla(negocioId, plantilla, ahora);
        proyecto.agregarColumnas(request.columnasExtra(), ahora);
        renombrarSiEligioUnNombre(proyecto, request.nombre(), ahora);
        return proyectoRepository.saveConEstructura(proyecto);
    }

    private static void renombrarSiEligioUnNombre(Proyecto proyecto, String nombre, OffsetDateTime ahora) {
        if (nombre != null && !nombre.isBlank()) {
            proyecto.renombrar(nombre, ahora);
        }
    }
}
