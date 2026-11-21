package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLectura {

    private final NegocioRepository negocioRepository;
    private final ProcesamientoRepository procesamientoRepository;

    public LecturaItem execute(UsuarioLoteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return LecturaItem.de(obtenerEstado(negocioId, request.loteId()));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private EstadoLectura obtenerEstado(NegocioId negocioId, LoteId loteId) {
        return procesamientoRepository.obtenerEstado(negocioId, loteId);
    }
}
