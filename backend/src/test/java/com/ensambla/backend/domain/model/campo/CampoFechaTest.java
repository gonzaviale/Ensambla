package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class CampoFechaTest extends CampoDeCualquierTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.FECHA;
    }

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of("2026-10-09");
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of("2026-02-31", "09/10/2026", 20261009);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Tiene que ser una fecha válida";
    }

    @Test
    @DisplayName("En el reporte el valor va como fecha")
    void enElReporteVaComoFecha() {
        cuandoSePideLaCeldaDeReporte("2026-10-09");

        entoncesElResultadoEs(LocalDate.of(2026, 10, 9));
    }
}
