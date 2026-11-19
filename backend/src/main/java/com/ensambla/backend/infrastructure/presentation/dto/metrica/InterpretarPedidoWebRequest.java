package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que la persona escribió para pedir una métrica, por ejemplo «cuánto vendí esta semana». */
public record InterpretarPedidoWebRequest(@NotBlank @Size(max = 200) String pedido) {
}
