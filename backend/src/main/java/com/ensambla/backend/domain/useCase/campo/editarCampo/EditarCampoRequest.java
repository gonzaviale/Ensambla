package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/** Reemplaza toda la configuración del campo: lo que no viene vuelve a su valor por defecto. */
public record EditarCampoRequest(
        UsuarioId usuarioId,
        CampoId campoId,
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
