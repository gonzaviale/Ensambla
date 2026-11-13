package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProgresoObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProgresoObjetivoResponse.MiniProgresoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProgresoObjetivoResponse.PuntoSemanalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProgresoObjetivoPresentationMapper {

    private final MiniObjetivoPresentationMapper miniObjetivoPresentationMapper;

    public ObtenerProgresoRequest toRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
        return new ObtenerProgresoRequest(usuarioId, objetivoId);
    }

    public ProgresoObjetivoResponse toResponse(ObtenerProgresoResponse response) {
        return new ProgresoObjetivoResponse(
                response.meta(),
                response.hito(),
                toResponse(response.semanas()),
                response.minis().stream().map(this::toResponse).toList());
    }

    private MiniProgresoResponse toResponse(MiniProgreso item) {
        return new MiniProgresoResponse(
                miniObjetivoPresentationMapper.toResponse(item.mini()), item.hito(), toResponse(item.semanas()));
    }

    private List<PuntoSemanalResponse> toResponse(List<PuntoSemanal> puntos) {
        return puntos.stream()
                .map(punto -> new PuntoSemanalResponse(punto.semana(), punto.valor(), punto.estimado()))
                .toList();
    }
}
