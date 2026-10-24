package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EliminarProyecto {

    private final NegocioService negocioService;

    private final ProyectoService proyectoService;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarProyectoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        transaccionPort.ejecutar(() -> eliminar(negocioId, request.proyectoId()));
    }

    private void eliminar(NegocioId negocioId, ProyectoId proyectoId) {
        proyectoService.obtener(negocioId, proyectoId);
        proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }
}

