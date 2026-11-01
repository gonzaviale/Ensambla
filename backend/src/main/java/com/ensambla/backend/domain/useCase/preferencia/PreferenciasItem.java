package com.ensambla.backend.domain.useCase.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;

import java.util.List;
import java.util.Map;

public record PreferenciasItem(
        List<String> descartadas,
        Map<String, Boolean> grupos,
        boolean bienvenidaVista,
        String nombre) {

    public static PreferenciasItem de(PreferenciaUsuario preferencia) {
        return new PreferenciasItem(
                preferencia.getDescartadas(),
                preferencia.getGrupos(),
                preferencia.isBienvenidaVista(),
                preferencia.getNombre());
    }
}
