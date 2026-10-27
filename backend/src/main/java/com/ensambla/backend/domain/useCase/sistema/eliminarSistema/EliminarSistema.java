package com.ensambla.backend.domain.useCase.sistema.eliminarSistema;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Elimina el sistema y todo lo que contiene. Es idempotente: si no existe, o es de otro negocio, no hace nada, así la
 * pantalla puede reintentar un borrado sin que falle.
 */
@RequiredArgsConstructor
public class EliminarSistema {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarSistemaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        ProyectoId proyectoId = IdSistema.aId(negocioId, request.id(), ProyectoId::de);
        transaccionPort.ejecutar(() -> proyectoRepository.deleteByIdAndNegocioId(proyectoId, negocioId));
    }
}
