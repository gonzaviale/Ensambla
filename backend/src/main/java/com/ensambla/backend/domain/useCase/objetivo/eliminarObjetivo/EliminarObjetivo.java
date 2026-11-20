package com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EliminarObjetivo {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarObjetivoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        eliminarEnTransaccion(negocioId, request.objetivoId());
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void eliminarEnTransaccion(NegocioId negocioId, ObjetivoId objetivoId) {
        transaccionPort.ejecutar(() -> eliminar(negocioId, objetivoId));
    }

    private void eliminar(NegocioId negocioId, ObjetivoId objetivoId) {
        objetivoRepository.obtener(negocioId, objetivoId);
        objetivoRepository.deleteByIdAndNegocioId(objetivoId, negocioId);
    }
}
