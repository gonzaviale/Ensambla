package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProyectoItem(
        UUID id,
        String nombre,
        String descripcion,
        UUID plantillaId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        List<BloqueItem> bloques
) {

    public static ProyectoItem de(Proyecto proyecto) {
        return new ProyectoItem(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getPlantillaId(),
                proyecto.getCreadoEn(),
                proyecto.getActualizadoEn(),
                proyecto.getBloques().stream().map(BloqueItem::de).toList());
    }

    public record BloqueItem(UUID id, String nombre, Short posicion, List<CampoItem> campos) {

        static BloqueItem de(Bloque bloque) {
            return new BloqueItem(
                    bloque.getId(),
                    bloque.getNombre(),
                    bloque.getPosicion(),
                    bloque.getCampos().stream().map(CampoItem::de).toList());
        }
    }

    public record CampoItem(
            UUID id,
            String nombre,
            String tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones,
            UUID bloqueRelacionadoId,
            String formula,
            Short posicion
    ) {

        static CampoItem de(Campo campo) {
            return new CampoItem(
                    campo.getId(),
                    campo.getNombre(),
                    campo.getTipo().name(),
                    campo.getObligatorio(),
                    campo.getValorInicial(),
                    campo.getOpciones(),
                    campo.getBloqueRelacionadoId(),
                    campo.getFormula(),
                    campo.getPosicion());
        }
    }
}

