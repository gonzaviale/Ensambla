package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarBloqueUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    public void execute(EliminarBloqueRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        bloqueService.eliminar(bloque);
    }
}
