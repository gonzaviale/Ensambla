package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiccionarioSamiTest {

    @Test
    @DisplayName("Un diccionario sin listas las deja vacías en vez de nulas")
    void unDiccionarioSinListasLasDejaVacias() {
        DiccionarioSami diccionario = new DiccionarioSami(null, null, null);

        entoncesEstanVacias(diccionario.plantillas(), diccionario.columnas(), diccionario.preguntas());
    }

    @Test
    @DisplayName("Una plantilla del diccionario sin términos los deja vacíos")
    void unaPlantillaSinTerminosLosDejaVacios() {
        PlantillaDiccionario plantilla = new PlantillaDiccionario("Ventas", null);

        entoncesEstanVacias(plantilla.terminos());
    }

    @Test
    @DisplayName("Una columna del diccionario sin opciones, disparadores ni plantillas los deja vacíos")
    void unaColumnaSinListasLasDejaVacias() {
        ColumnaDiccionario columna = new ColumnaDiccionario("Saldo", null, null, null, null);

        entoncesEstanVacias(columna.opciones(), columna.disparadores(), columna.plantillas());
    }

    private static void entoncesEstanVacias(List<?>... listas) {
        assertThat(listas).allSatisfy(lista -> assertThat(lista).isEmpty());
    }
}