package com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ConfirmarVerificacionWhatsappRequest(UsuarioId usuarioId, String codigo) {
}
