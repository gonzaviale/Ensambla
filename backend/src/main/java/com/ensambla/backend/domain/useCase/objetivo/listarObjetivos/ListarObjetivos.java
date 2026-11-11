package com.ensambla.backend.domain.useCase.objetivo.listarObjetivos;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarObjetivos {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    public List<ObjetivoItem> execute(ListarObjetivosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return objetivoRepository.findAllByNegocioId(negocioId).stream().map(ObjetivoItem::de).toList();
    }
}
