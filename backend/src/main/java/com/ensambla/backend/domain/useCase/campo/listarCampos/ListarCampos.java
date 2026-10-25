package com.ensambla.backend.domain.useCase.campo.listarCampos;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarCampos {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    public List<CampoItem> execute(ListarCamposRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
        return campos.stream().map(CampoItem::de).toList();
    }
}
