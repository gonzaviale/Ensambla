package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        eliminarEnTransaccion(negocioId, request.proyectoId());
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void eliminarEnTransaccion(NegocioId negocioId, ProyectoId proyectoId) {
        transaccionPort.ejecutar(() -> eliminar(negocioId, proyectoId));
    }

    private void eliminar(NegocioId negocioId, ProyectoId proyectoId) {
        exigirQueExista(negocioId, proyectoId);
        borrar(negocioId, proyectoId);
    }

    private void exigirQueExista(NegocioId negocioId, ProyectoId proyectoId) {
        proyectoRepository.obtener(negocioId, proyectoId);
    }

    private void borrar(NegocioId negocioId, ProyectoId proyectoId) {
        proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId);
    }
}
