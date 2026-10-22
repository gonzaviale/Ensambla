package com.ensambla.backend.domain.mapper.preferencia;

import com.ensambla.backend.domain.dto.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;

public class PreferenciasItemMapper {

    public PreferenciasItem toResponse(PreferenciaUsuario preferencia) {
        return new PreferenciasItem(
                preferencia.getDescartadas(),
                preferencia.getGrupos(),
                preferencia.isBienvenidaVista(),
                preferencia.getNombre());
    }
}
