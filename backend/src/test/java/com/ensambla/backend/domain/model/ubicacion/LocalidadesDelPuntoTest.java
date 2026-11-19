package com.ensambla.backend.domain.model.ubicacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalidadesDelPuntoTest {

    private LocalidadesDelPunto localidades;

    @Test
    @DisplayName("La principal es la primera que reconoce el mapa")
    void laPrincipalEsLaPrimeraQueReconoceElMapa() {
        dadoQueElMapaReconoce("San Justo", "La Matanza");

        entoncesLaPrincipalEs("San Justo");
    }

    @Test
    @DisplayName("Sin nombres reconocidos no hay localidad principal")
    void sinNombresReconocidosNoHayLocalidadPrincipal() {
        dadoQueElMapaNoReconoceNada();

        entoncesNoHayLocalidadPrincipal();
    }

    @ParameterizedTest
    @ValueSource(strings = {"San Justo", "san justo", "SAN JUSTO", "  San   Justo  ", "Ramos Mejia", "ramos mejía"})
    @DisplayName("Admite el nombre sin importar mayúsculas, tildes ni espacios de más")
    void admiteElNombreSinImportarMayusculasTildesNiEspaciosDeMas(String localidad) {
        dadoQueElMapaReconoce("San Justo", "Ramos Mejía");

        entoncesAdmite(localidad);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ramos", "Lomas del Mirador", "San Justo Oeste", ""})
    @DisplayName("Rechaza una localidad que el mapa no reconoce en ese punto")
    void rechazaUnaLocalidadQueElMapaNoReconoceEnEsePunto(String localidad) {
        dadoQueElMapaReconoce("San Justo", "Ramos Mejía");

        entoncesNoAdmite(localidad);
    }

    @Test
    @DisplayName("Si el mapa no reconoce ninguna localidad no hay nada que contradecir")
    void siElMapaNoReconoceNingunaLocalidadNoHayNadaQueContradecir() {
        dadoQueElMapaNoReconoceNada();

        entoncesAdmite("Cualquier lugar");
    }

    // --- dado ---

    private void dadoQueElMapaReconoce(String... nombres) {
        localidades = new LocalidadesDelPunto(List.of(nombres));
    }

    private void dadoQueElMapaNoReconoceNada() {
        localidades = new LocalidadesDelPunto(List.of());
    }

    // --- entonces ---

    private void entoncesLaPrincipalEs(String nombre) {
        assertThat(localidades.principal()).contains(nombre);
    }

    private void entoncesNoHayLocalidadPrincipal() {
        assertThat(localidades.principal()).isEmpty();
    }

    private void entoncesAdmite(String localidad) {
        assertThat(localidades.admite(localidad)).isTrue();
    }

    private void entoncesNoAdmite(String localidad) {
        assertThat(localidades.admite(localidad)).isFalse();
    }
}
