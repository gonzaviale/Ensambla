package com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** @param telefono el número como lo escribió la persona, con código de país */
public record SolicitarVerificacionWhatsappRequest(UsuarioId usuarioId, String telefono) {
}
