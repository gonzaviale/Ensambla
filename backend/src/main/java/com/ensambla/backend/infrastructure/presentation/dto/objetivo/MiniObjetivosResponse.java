package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;

import java.util.List;

public record MiniObjetivosResponse(
        List<MiniObjetivoResponse> minis,
        List<MiniObjetivoResponse> historial,
        FaltanDatosMiniObjetivos faltanDatos) {
}
