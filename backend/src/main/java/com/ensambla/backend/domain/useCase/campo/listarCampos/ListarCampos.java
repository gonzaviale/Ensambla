package com.ensambla.backend.domain.useCase.campo.listarCampos;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarCampos {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    public List<CampoItem> execute(ListarCamposRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        return campos.stream().map(CampoItem::de).toList();
    }
}
