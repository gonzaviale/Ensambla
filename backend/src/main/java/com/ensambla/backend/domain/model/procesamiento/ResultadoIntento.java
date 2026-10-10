package com.ensambla.backend.domain.model.procesamiento;

public record ResultadoIntento(PropuestaLectura propuesta, String error, boolean recuperable) {
}
