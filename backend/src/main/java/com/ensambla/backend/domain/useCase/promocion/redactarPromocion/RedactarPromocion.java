package com.ensambla.backend.domain.useCase.promocion.redactarPromocion;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.promocion.CanalPromocion;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.promocion.Promocion;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

/** Escribe la promoción elegida con el tono y el largo del canal donde la persona la va a compartir. */
@RequiredArgsConstructor
public class RedactarPromocion {

    private final NegocioRepository negocioRepository;

    private final CatalogoDeCanales catalogoDeCanales;

    public RedactarPromocionResponse execute(RedactarPromocionRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        CanalPromocion canal = catalogoDeCanales.porClave(request.canal());
        Promocion promocion = request.promocion();
        return new RedactarPromocionResponse(canal.redactar(negocio.getNombre(), promocion));
    }
}
