package com.ensambla.backend.domain.useCase.sistema.listarSistemas;

import com.ensambla.backend.domain.model.negocio.NegocioId;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return sistemaRepository.findAllByNegocioId(negocioId).stream().map(SistemaItem::de).toList();
    }
}
