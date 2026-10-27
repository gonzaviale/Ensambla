package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

public record CrearCampoRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        BloqueId bloqueRelacionadoId,
        String formula
) {

    public ConfiguracionCampo aConfiguracionCampo() {
        return new ConfiguracionCampo(nombre, tipo, obligatorio, valorInicial, opciones, bloqueRelacionadoId, formula);
    }
}
