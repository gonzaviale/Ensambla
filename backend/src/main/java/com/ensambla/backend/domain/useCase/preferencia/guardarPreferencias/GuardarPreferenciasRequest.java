package com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias;

import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;
import java.util.Map;

public record GuardarPreferenciasRequest(
        UsuarioId usuarioId,
        List<String> descartadas,
        Map<String, Boolean> grupos,
        boolean bienvenidaVista,
        String nombre) {
}
