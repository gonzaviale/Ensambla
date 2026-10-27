package com.ensambla.backend.domain.useCase.negocio.obtenerConfiguracionNegocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.useCase.negocio.ConfiguracionNegocioResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerConfiguracionNegocio {

    private final NegocioRepository negocioRepository;
    private final ZonaRepository zonaRepository;

    public ConfiguracionNegocioResponse execute(ObtenerConfiguracionNegocioRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        return ConfiguracionNegocioResponse.de(negocio,
                zonaRepository.findByIdSiTiene(negocio.getZonaId()).orElse(null));
    }
}
