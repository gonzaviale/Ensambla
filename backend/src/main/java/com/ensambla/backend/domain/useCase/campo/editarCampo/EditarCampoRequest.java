package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;

import java.util.List;
import java.util.UUID;

/** Reemplaza toda la configuración del campo: lo que no viene vuelve a su valor por defecto. */
public record EditarCampoRequest(
        UUID usuarioId,
        UUID campoId,
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
