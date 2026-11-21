package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NombreDeProductoTest {

    private boolean esElMismo;

    @Test
    @DisplayName("Es el mismo producto aunque el de la zona agregue tamaño o marca")
    void esElMismoProductoAunqueElDeLaZonaAgregueTamanioOMarca() {
        cuandoSeComparan("Yerba", "Yerba Playadito 1 kg");

        entoncesEsElMismoProducto();
    }

    @Test
    @DisplayName("Es el mismo producto aunque cambien las tildes y las mayúsculas")
    void esElMismoProductoAunqueCambienLasTildesYLasMayusculas() {
        cuandoSeComparan("Azúcar Ledesma 1 kg", "AZUCAR común 500 g");

        entoncesEsElMismoProducto();
    }

    @Test
    @DisplayName("Es el mismo producto aunque uno esté en singular y el otro en plural")
    void esElMismoProductoAunqueUnoEsteEnSingularYElOtroEnPlural() {
        cuandoSeComparan("Fideo guisero", "Fideos Matarazzo 500 g");

        entoncesEsElMismoProducto();
    }

    @Test
    @DisplayName("No es el mismo producto si la primera palabra del negocio no está en el de la zona")
    void noEsElMismoProductoSiLaPrimeraPalabraDelNegocioNoEstaEnElDeLaZona() {
        cuandoSeComparan("Yerba Playadito", "Azúcar Ledesma");

        entoncesNoEsElMismoProducto();
    }

    @Test
    @DisplayName("Las palabras vacías no cuentan para comparar")
    void lasPalabrasVaciasNoCuentanParaComparar() {
        cuandoSeComparan("Pack 6 de gaseosas", "Pack de agua");

        entoncesNoEsElMismoProducto();
    }

    @Test
    @DisplayName("Un nombre que sólo tiene números y unidades no se parece a ningún producto")
    void unNombreQueSoloTieneNumerosYUnidadesNoSePareceANingunProducto() {
        cuandoSeComparan("1 kg", "Yerba 1 kg");

        entoncesNoEsElMismoProducto();
    }

    @Test
    @DisplayName("Un nombre vacío no se parece a ningún producto")
    void unNombreVacioNoSePareceANingunProducto() {
        cuandoSeComparan("", "Yerba");

        entoncesNoEsElMismoProducto();
    }

    // --- cuando ---
    private void cuandoSeComparan(String nombreDelNegocio, String nombreDeLaZona) {
        esElMismo = NombreDeProducto.esElMismo(nombreDelNegocio, nombreDeLaZona);
    }

    // --- entonces ---
    private void entoncesEsElMismoProducto() {
        assertThat(esElMismo).isTrue();
    }

    private void entoncesNoEsElMismoProducto() {
        assertThat(esElMismo).isFalse();
    }
}
