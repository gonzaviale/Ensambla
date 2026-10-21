package com.ensambla.backend.domain.model.ubicacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalidadesDelPuntoTest {

    @Test
    @DisplayName("La principal es la primera que reconoce el mapa")
    void principalShouldBeTheFirstName() {
        assertThat(new LocalidadesDelPunto(List.of("San Justo", "La Matanza")).principal()).contains("San Justo");
    }

    @Test
    @DisplayName("Sin nombres reconocidos no hay localidad principal")
    void principalShouldBeEmptyWithoutNames() {
        assertThat(new LocalidadesDelPunto(List.of()).principal()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"San Justo", "san justo", "SAN JUSTO", "  San   Justo  ", "Ramos Mejia", "ramos mejía"})
    @DisplayName("Admite el nombre sin importar mayúsculas, tildes ni espacios de más")
    void admiteShouldIgnoreCaseAccentsAndSpaces(String localidad) {
        LocalidadesDelPunto localidades = new LocalidadesDelPunto(List.of("San Justo", "Ramos Mejía"));

        assertThat(localidades.admite(localidad)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ramos", "Lomas del Mirador", "San Justo Oeste", ""})
    @DisplayName("Rechaza una localidad que el mapa no reconoce en ese punto")
    void admiteShouldRejectOtherLocalities(String localidad) {
        LocalidadesDelPunto localidades = new LocalidadesDelPunto(List.of("San Justo", "Ramos Mejía"));

        assertThat(localidades.admite(localidad)).isFalse();
    }

    @Test
    @DisplayName("Si el mapa no reconoce ninguna localidad no hay nada que contradecir")
    void admiteShouldAcceptAnythingWhenMapKnowsNone() {
        assertThat(new LocalidadesDelPunto(List.of()).admite("Cualquier lugar")).isTrue();
    }
}
