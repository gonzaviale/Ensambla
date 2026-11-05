package com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Prende o apaga el resumen de la mañana por WhatsApp de la cuenta de la sesión. */
public record ConfigurarAvisosPorWhatsappRequest(UsuarioId usuarioId, boolean activo) {
}
