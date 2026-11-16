package com.ensambla.backend.domain.useCase.cartel.sugerirPromociones;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.cartel.FuenteDePromocion;
import com.ensambla.backend.domain.model.cartel.PromocionSugerida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.cartel.PromocionSugeridaItem;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/** Sugiere qué promocionar con lo que el motor de avisos ya encontró en el negocio. */
@RequiredArgsConstructor
public class SugerirPromociones {

    private final NegocioRepository negocioRepository;

    private final AvisoRepository avisoRepository;

    private final List<FuenteDePromocion> fuentes;

    public SugerirPromocionesResponse execute(SugerirPromocionesRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return new SugerirPromocionesResponse(fuentes.stream()
                .flatMap(fuente -> promocionesDe(fuente, negocioId))
                .map(PromocionSugeridaItem::de)
                .toList());
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Stream<PromocionSugerida> promocionesDe(FuenteDePromocion fuente, NegocioId negocioId) {
        return avisosPendientesDe(fuente, negocioId).stream().flatMap(aviso -> fuente.de(aviso).stream());
    }

    private List<Aviso> avisosPendientesDe(FuenteDePromocion fuente, NegocioId negocioId) {
        return avisoRepository.findByNegocioIdAndTipoAndEstado(negocioId, fuente.tipoDeAviso(), EstadoAviso.PENDIENTE);
    }
}
