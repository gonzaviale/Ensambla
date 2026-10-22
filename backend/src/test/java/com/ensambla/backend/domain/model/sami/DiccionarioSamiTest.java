package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiccionarioSamiTest {

    @Test
    @DisplayName("Las listas ausentes quedan vacías en vez de nulas")
    void constructorShouldReplaceMissingListsWithEmptyOnes() {
        DiccionarioSami diccionario = new DiccionarioSami(null, null, null);
        PlantillaDiccionario plantilla = new PlantillaDiccionario("Ventas", null);
        ColumnaDiccionario columna = new ColumnaDiccionario("Saldo", null, null, null, null);

        assertThat(diccionario.plantillas()).isEmpty();
        assertThat(diccionario.columnas()).isEmpty();
        assertThat(diccionario.preguntas()).isEmpty();
        assertThat(plantilla.terminos()).isEmpty();
        assertThat(columna.opciones()).isEmpty();
        assertThat(columna.disparadores()).isEmpty();
        assertThat(columna.plantillas()).isEmpty();
    }
}
