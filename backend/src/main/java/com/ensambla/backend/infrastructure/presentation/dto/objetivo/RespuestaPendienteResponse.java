package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

/** Un mini-objetivo manual que espera la respuesta de la persona, con el objetivo al que pertenece. */
public record RespuestaPendienteResponse(
        ObjetivoId objetivoId, String objetivoTexto, MiniObjetivoResponse miniObjetivo) {
}
