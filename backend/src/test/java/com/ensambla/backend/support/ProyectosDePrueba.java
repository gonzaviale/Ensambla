package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.proyecto.ProyectoItem;
import com.ensambla.backend.domain.useCase.proyecto.crearProyecto.CrearProyecto;
import com.ensambla.backend.domain.useCase.proyecto.crearProyecto.CrearProyectoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Crea proyectos con los casos de uso reales, contra PostgreSQL y en el instante que elige cada test. El negocio se
 * indica directo (en producción sale de la sesión).
 */
@Component
@RequiredArgsConstructor
public class ProyectosDePrueba {

    private final ProyectoRepository proyectoRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    public Proyecto crear(NegocioId negocioId, String nombre, String descripcion, OffsetDateTime ahora) {
        ProyectoItem creado = new CrearProyecto(
                negocioPropio(negocioId), proyectoRepository, controlPlanService, transaccionPort, reloj(ahora))
                .execute(new CrearProyectoRequest(usuarioDe(negocioId), nombre, descripcion));
        return proyectoRepository.obtener(negocioId, creado.id());
    }

    /** Copia la plantilla sin controlar el cupo del plan, para poder armar varios proyectos de un mismo negocio. */
    public Proyecto crearDesdePlantilla(NegocioId negocioId, PlantillaProyecto plantilla, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(
                () -> proyectoRepository.saveConEstructura(Proyecto.desdePlantilla(negocioId, plantilla, ahora)));
    }

    private static Clock reloj(OffsetDateTime ahora) {
        return Clock.fixed(ahora.toInstant(), ahora.getOffset());
    }

    /** El usuario de prueba comparte el UUID con su negocio. */
    private static UsuarioId usuarioDe(NegocioId negocioId) {
        return UsuarioId.de(negocioId.valor());
    }

    private static NegocioRepository negocioPropio(NegocioId negocioId) {
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(usuarioDe(negocioId))).thenReturn(negocioId);
        return negocioRepository;
    }
}
