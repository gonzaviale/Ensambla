package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NombreDeProductoTest {

    @Test
    @DisplayName("Es el mismo producto aunque cambie el tamaño, la marca extra, las tildes o el plural")
    void esElMismoShouldIgnoreSizeAccentsAndPlural() {
        assertThat(NombreDeProducto.esElMismo("Yerba", "Yerba Playadito 1 kg")).isTrue();
        assertThat(NombreDeProducto.esElMismo("Azúcar Ledesma 1 kg", "AZUCAR común 500 g")).isTrue();
        assertThat(NombreDeProducto.esElMismo("Fideo guisero", "Fideos Matarazzo 500 g")).isTrue();
    }

    @Test
    @DisplayName("No es el mismo producto si la primera palabra del negocio no está en el de la zona")
    void esElMismoShouldBeFalseWhenFirstWordIsMissing() {
        assertThat(NombreDeProducto.esElMismo("Yerba Playadito", "Azúcar Ledesma")).isFalse();
    }

    @Test
    @DisplayName("Las palabras vacías, los números y las muy cortas no cuentan para comparar")
    void esElMismoShouldIgnoreEmptyWords() {
        assertThat(NombreDeProducto.esElMismo("Pack 6 de gaseosas", "Pack de agua")).isFalse();
        assertThat(NombreDeProducto.esElMismo("1 kg", "Yerba 1 kg")).isFalse();
        assertThat(NombreDeProducto.esElMismo("", "Yerba")).isFalse();
    }
}
