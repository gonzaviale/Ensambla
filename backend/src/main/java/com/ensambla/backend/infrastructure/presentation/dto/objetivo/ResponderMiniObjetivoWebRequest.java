package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo que la persona responde a un mini-objetivo manual: si lo hizo y qué pasó, con un comentario opcional. */
public record ResponderMiniObjetivoWebRequest(
        @NotNull(message = "Contá si lo hiciste") HizoManual hizo,
        @NotNull(message = "Elegí qué pasó") QuePaso quePaso,
        @Size(max = RespuestaMiniObjetivo.LARGO_MAXIMO_COMENTARIO,
                message = "El comentario no puede tener más de 300 caracteres") String comentario) {
}
