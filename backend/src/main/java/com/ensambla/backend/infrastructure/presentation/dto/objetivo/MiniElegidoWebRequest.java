package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.AccionManual;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un mini-objetivo que la persona aceptó. Si es medido se manda cuál es ({@code tipo}, {@code registroId},
 * {@code referencia}, tal cual vino en la propuesta); si es manual, su texto.
 */
public record MiniElegidoWebRequest(
        @NotNull(message = "El modo es obligatorio") ModoMiniObjetivo modo,
        TipoMiniObjetivo tipo,
        RegistroId registroId,
        @Size(max = 200, message = "La referencia es demasiado larga") String referencia,
        @Size(max = AccionManual.LARGO_MAXIMO_TITULO,
                message = "El título puede tener hasta 80 caracteres") String titulo,
        @Size(max = AccionManual.LARGO_MAXIMO_JUSTIFICACION,
                message = "La justificación puede tener hasta 280 caracteres") String justificacion) {
}
