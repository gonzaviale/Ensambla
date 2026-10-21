package com.ensambla.backend.domain.mapper.sami;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.sami.ColumnaSamiItem;
import com.ensambla.backend.domain.mapper.proyecto.ProyectoItemMapper;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrearProyectoDesdeSamiMapperTest {

    private final CrearProyectoDesdeSamiMapper mapper = new CrearProyectoDesdeSamiMapper(new ProyectoItemMapper());

    @Test
    @DisplayName("Convierte las columnas aceptadas en configuraciones de campo opcionales y sin valor inicial")
    void toColumnasShouldBuildFieldConfigurations() {
        List<ConfiguracionCampo> result = mapper.toColumnas(List.of(
                new ColumnaSamiItem("Forma de pago", "OPCION", List.of("Efectivo", "Tarjeta")),
                new ColumnaSamiItem("Saldo", "NUMERO", null)));

        assertThat(result).containsExactly(
                new ConfiguracionCampo("Forma de pago", "OPCION", false, null, List.of("Efectivo", "Tarjeta"), null, null),
                new ConfiguracionCampo("Saldo", "NUMERO", false, null, null, null, null));
    }

    @Test
    @DisplayName("Arma la respuesta con el proyecto, sus bloques y campos")
    void toResponseShouldMapProject() {
        ProyectoItem result = mapper.toResponse(ProyectoTestData.proyecto());

        assertThat(result.nombre()).isEqualTo("Inventario");
        assertThat(result.bloques()).extracting(ProyectoItem.BloqueItem::nombre).containsExactly("Productos", "Vacío");
    }
}
