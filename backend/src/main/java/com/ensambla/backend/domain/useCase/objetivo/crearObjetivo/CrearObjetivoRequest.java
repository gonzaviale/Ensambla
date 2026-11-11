package com.ensambla.backend.domain.useCase.objetivo.crearObjetivo;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;

public record CrearObjetivoRequest(UsuarioId usuarioId, String texto, BigDecimal meta) {
}
