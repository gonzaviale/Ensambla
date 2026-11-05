package com.ensambla.backend.infrastructure.presentation.dto.preferencia;

import jakarta.validation.constraints.NotNull;

public record AvisosPorWhatsappWebRequest(@NotNull Boolean activo) {
}
