package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;
import java.util.List;

public record ProyectoItem(
        ProyectoId id,
        String nombre,
        String descripcion,
        PlantillaId plantillaId,
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

    public record BloqueItem(BloqueId id, String nombre, Short posicion, List<CampoItem> campos) {

        static BloqueItem de(Bloque bloque) {
            return new BloqueItem(
                    bloque.getId(),
                    bloque.getNombre(),
                    bloque.getPosicion(),
                    bloque.getCampos().stream().map(CampoItem::de).toList());
        }
    }

    public record CampoItem(
            CampoId id,
            String nombre,
            String tipo,
            Boolean obligatorio,
            String valorInicial,
            List<String> opciones,
            BloqueId bloqueRelacionadoId,
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

