package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;

import java.util.List;
import java.util.UUID;

/**
 * La propuesta final de Sami con los ajustes de la persona.
 *
 * @param nombre   opcional; sin él, el proyecto se llama como la plantilla
 * @param columnas las columnas extra que la persona dejó
 */
public record CrearProyectoDesdeSamiRequest(
        UUID usuarioId,
        UUID plantillaId,
        String nombre,
        List<ColumnaSamiItem> columnas
) {

    public List<ConfiguracionCampo> columnasExtra() {
        return columnas.stream()
                .map(columna -> new ConfiguracionCampo(
                        columna.nombre(), columna.tipo(), false, null, columna.opciones(), null, null))
                .toList();
    }
}
