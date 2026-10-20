package com.ensambla.backend.domain.service.ubicacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RubroServiceTest {

    @Test
    @DisplayName("El catálogo conserva los códigos admitidos por la base y sus nombres legibles")
    void listarShouldReturnSupportedRubros() {
        var rubros = new RubroService().listar();

        assertThat(rubros).extracting(Enum::name)
                .containsExactly("ALMACEN", "KIOSCO", "MINIMERCADO", "DIETETICA", "OTRO");
        assertThat(rubros).extracting("nombre")
                .containsExactly("Almacén", "Kiosco", "Minimercado", "Dietética", "Otro");
    }
}
