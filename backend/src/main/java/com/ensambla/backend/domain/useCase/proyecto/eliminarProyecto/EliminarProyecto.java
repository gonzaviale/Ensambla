package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EliminarProyecto {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarProyectoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        transaccionPort.ejecutar(() -> eliminar(negocioId, request.proyectoId()));
    }

    private void eliminar(NegocioId negocioId, ProyectoId proyectoId) {
        proyectoRepository.obtener(negocioId, proyectoId);
        proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }
}

