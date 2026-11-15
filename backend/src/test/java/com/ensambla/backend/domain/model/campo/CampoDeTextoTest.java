package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class CampoDeTextoTest extends CampoPorTipoTest {

    @Override
    protected List<Object> valoresValidos() {
        return List.of("Ana", "x".repeat(1000));
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of(12, true);
    }

    @Override
    protected String mensajeDeError() {
        return "Tiene que ser un texto";
    }

    @Test
    @DisplayName("Rechaza un texto que supera los 1000 caracteres")
    void errorDeShouldReportWhenTextoIsTooLong() {
        assertThat(campo().errorDe("x".repeat(1001))).contains("Máximo 1000 caracteres");
    }

    @Test
    @DisplayName("Guarda el texto tal cual, sin convertirlo")
    void tiparShouldKeepTheText() {
        assertThat(campo().tipar("Ana")).isEqualTo("Ana");
    }

    @Test
    @DisplayName("En el reporte va como texto")
    void celdaDeReporteShouldBeTheText() {
        assertThat(campo().celdaDeReporte("Ana", id -> "otra fila")).isEqualTo("Ana");
    }
}
