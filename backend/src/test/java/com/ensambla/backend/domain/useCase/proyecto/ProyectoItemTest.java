package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProyectoItemTest {

    @Test
    @DisplayName("Mapea todos los datos y el orden de los bloques y campos")
    void deShouldMapAllFields() {
        Proyecto proyecto = ProyectoTestData.proyecto();

        var result = ProyectoItem.de(proyecto);

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        assertThat(result.nombre()).isEqualTo(proyecto.getNombre());
        assertThat(result.descripcion()).isEqualTo(proyecto.getDescripcion());
        assertThat(result.plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.creadoEn()).isEqualTo(AHORA);
        assertThat(result.actualizadoEn()).isEqualTo(AHORA);
        assertThat(result.bloques()).hasSize(2);
        var bloque = result.bloques().getFirst();
        assertThat(bloque.id()).isEqualTo(proyecto.getBloques().getFirst().getId());
        assertThat(bloque.nombre()).isEqualTo("Productos");
        assertThat(bloque.posicion()).isZero();
        assertThat(bloque.campos()).hasSize(2);
        var campo = bloque.campos().getFirst();
        assertThat(campo.id()).isEqualTo(proyecto.getBloques().getFirst().getCampos().getFirst().getId());
        assertThat(campo.nombre()).isEqualTo("Nombre");
        assertThat(campo.tipo()).isEqualTo("TEXTO");
        assertThat(campo.obligatorio()).isTrue();
        assertThat(campo.valorInicial()).isNull();
        assertThat(campo.opciones()).isEmpty();
        assertThat(campo.bloqueRelacionadoId()).isNull();
        assertThat(campo.posicion()).isZero();
        assertThat(result.bloques().get(1).campos()).isEmpty();
    }
}

