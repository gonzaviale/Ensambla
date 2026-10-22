package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.dto.zona.ObtenerZonaRequest;
import com.ensambla.backend.domain.dto.zona.ZonaItem;
import com.ensambla.backend.domain.mapper.zona.ZonaItemMapper;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.zona.AccesoZonaService;
import com.ensambla.backend.domain.service.zona.AgregadoZonaService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerZonaUseCase {

    private final NegocioService negocioService;

    private final ZonaService zonaService;

    private final AgregadoZonaService agregadoZonaService;

    private final ZonaItemMapper zonaItemMapper;

    private final AccesoZonaService accesoZonaService;

    private final Clock clock;

    public ZonaItem execute(ObtenerZonaRequest request) {
        Negocio negocio = negocioService.obtenerConfiguracion(request.usuarioId());
        AnalisisZona analisis = zonaService.obtener(negocio.getZonaId())
                .map(agregadoZonaService::analizar)
                .orElseGet(AnalisisZona::sinZona);
        return accesoZonaService.filtrar(negocio.getId(), zonaItemMapper.toResponse(analisis), OffsetDateTime.now(clock));
    }
}
