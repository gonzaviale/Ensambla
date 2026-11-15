package com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.marca.KitDeMarcaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerKitDeMarca {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final Clock clock;

    public KitDeMarcaItem execute(ObtenerKitDeMarcaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return KitDeMarcaItem.de(obtenerKitDelNegocio(negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private KitDeMarca obtenerKitDelNegocio(NegocioId negocioId) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocioId, OffsetDateTime.now(clock));
    }
}
