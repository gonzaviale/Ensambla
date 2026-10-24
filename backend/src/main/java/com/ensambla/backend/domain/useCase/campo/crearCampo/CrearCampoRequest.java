package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;

import java.util.List;
import java.util.UUID;

public record CrearCampoRequest(
        UUID usuarioId,
        UUID bloqueId,
        String nombre,
        String tipo,
        Boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        UUID bloqueRelacionadoId,
        String formula
) {

    public ConfiguracionCampo aConfiguracionCampo() {
        return new ConfiguracionCampo(nombre, tipo, obligatorio, valorInicial, opciones, bloqueRelacionadoId, formula);
    }
}
