package com.ensambla.backend.domain.mapper.sami;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.mapper.proyecto.ProyectoItemMapper;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CrearProyectoDesdeSamiMapper {

    private final ProyectoItemMapper proyectoItemMapper;

    public List<ConfiguracionCampo> toColumnas(List<ColumnaSamiItem> columnas) {
        return columnas.stream()
                .map(columna -> new ConfiguracionCampo(
                        columna.nombre(), columna.tipo(), false, null, columna.opciones(), null, null))
                .toList();
    }

    public ProyectoItem toResponse(Proyecto proyecto) {
        return proyectoItemMapper.toResponse(proyecto);
    }
}
