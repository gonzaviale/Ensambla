package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoOpcionTest extends CampoPorTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.OPCION;
    }

    @Override
    protected List<Object> valoresValidos() {
        return List.of("Minorista", "Mayorista");
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of("Otra", true);
    }

    @Override
    protected String mensajeDeError() {
        return "Elegí una de las opciones";
    }

    @Test
    @DisplayName("Conoce las opciones entre las que se elige")
    void getOpcionesShouldBeTheOnesOfTheCampo() {
        assertThat(campo().getOpciones()).containsExactly("Minorista", "Mayorista");
    }
}
