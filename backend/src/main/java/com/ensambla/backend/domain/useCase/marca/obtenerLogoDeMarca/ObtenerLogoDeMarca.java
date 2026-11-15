package com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLogoDeMarca {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    public ObtenerLogoDeMarcaResponse execute(ObtenerLogoDeMarcaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        KitDeMarca kit = obtenerKitDelNegocio(negocioId);
        exigirQueTengaLogo(kit);
        return ObtenerLogoDeMarcaResponse.de(kit.getLogo());
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private KitDeMarca obtenerKitDelNegocio(NegocioId negocioId) {
        return kitDeMarcaRepository.findByNegocioId(negocioId).orElseThrow(this::logoInexistente);
    }

    private void exigirQueTengaLogo(KitDeMarca kit) {
        if (!kit.tieneLogo()) {
            throw logoInexistente();
        }
    }

    private ResourceNotFoundException logoInexistente() {
        return new ResourceNotFoundException("El negocio todavía no subió su logo");
    }
}
