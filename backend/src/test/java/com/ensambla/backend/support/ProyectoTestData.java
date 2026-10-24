package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.proyecto.Proyecto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ProyectoTestData {

    public static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final UUID NEGOCIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    public static final UUID PROYECTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");
    public static final UUID PLANTILLA_ID = UUID.fromString("00000000-0000-4000-8000-000000000004");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private ProyectoTestData() {
    }

    public static PlantillaProyecto plantilla() {
        return new PlantillaProyecto(PLANTILLA_ID, "Inventario", "Mis productos", new EstructuraPlantilla(List.of(
                new BloquePlantilla("Productos", List.of(
                        new CampoPlantilla("Nombre", TipoCampo.TEXTO, true, null, null),
                        new CampoPlantilla("Rubro", TipoCampo.OPCION, false, "Insumos", List.of("Insumos", "Otros")))),
                new BloquePlantilla("Vacío", List.of()))));
    }

    public static Proyecto proyecto() {
        Proyecto proyecto = Proyecto.desdePlantilla(NEGOCIO_ID, plantilla(), AHORA);
        proyecto.setId(PROYECTO_ID);
        proyecto.getBloques().forEach(bloque -> {
            bloque.setId(UUID.randomUUID());
            bloque.setProyectoId(PROYECTO_ID);
            bloque.getCampos().forEach(campo -> {
                campo.setId(UUID.randomUUID());
                campo.setBloqueId(bloque.getId());
            });
        });
        return proyecto;
    }
}

