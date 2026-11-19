package com.ensambla.backend.domain.useCase.sistema.listarSistemas;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarSistemas {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    public List<SistemaItem> execute(ListarSistemasRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return obtenerSistemas(negocioId).stream().map(SistemaItem::de).toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<Sistema> obtenerSistemas(NegocioId negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId);
    }
}
