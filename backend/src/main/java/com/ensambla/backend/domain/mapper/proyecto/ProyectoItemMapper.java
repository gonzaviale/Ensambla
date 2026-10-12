package com.ensambla.backend.domain.mapper.proyecto;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem.BloqueItem;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem.CampoItem;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;

public class ProyectoItemMapper {

    public ProyectoItem toResponse(Proyecto proyecto) {
        return new ProyectoItem(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getPlantillaId(),
                proyecto.getCreadoEn(),
                proyecto.getActualizadoEn(),
                proyecto.getBloques().stream().map(this::toResponse).toList());
    }

    private BloqueItem toResponse(Bloque bloque) {
        return new BloqueItem(
                bloque.getId(),
                bloque.getNombre(),
                bloque.getPosicion(),
                bloque.getCampos().stream().map(this::toResponse).toList());
    }

    private CampoItem toResponse(Campo campo) {
        return new CampoItem(
                campo.getId(),
                campo.getNombre(),
                campo.getTipo().name(),
                campo.getObligatorio(),
                campo.getValorInicial(),
                campo.getOpciones(),
                campo.getBloqueRelacionadoId(),
                campo.getPosicion());
    }
}

