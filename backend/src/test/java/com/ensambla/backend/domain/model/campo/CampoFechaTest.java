package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoFechaTest extends CampoPorTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.FECHA;
    }

    @Override
    protected List<Object> valoresValidos() {
        return List.of("2026-10-09");
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of("2026-02-31", "09/10/2026", 20261009);
    }

    @Override
    protected String mensajeDeError() {
        return "Tiene que ser una fecha válida";
    }

    @Test
    @DisplayName("En el reporte va como fecha")
    void celdaDeReporteShouldBeADate() {
        assertThat(campo().celdaDeReporte("2026-10-09", id -> "otra fila")).isEqualTo(LocalDate.of(2026, 10, 9));
    }
}
