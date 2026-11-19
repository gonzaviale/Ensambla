package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

abstract class CampoDeTextoTest extends CampoDeCualquierTipoTest {

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of("Ana", "x".repeat(1000));
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of(12, true);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Tiene que ser un texto";
    }

    @Test
    @DisplayName("Un texto de más de mil caracteres se rechaza")
    void unTextoDeMasDeMilCaracteresSeRechaza() {
        cuandoSeValida("x".repeat(1001));

        entoncesElErrorEs("Máximo 1000 caracteres");
    }

    @Test
    @DisplayName("El texto se guarda tal cual, sin convertirlo")
    void elTextoSeGuardaTalCualSinConvertirlo() {
        cuandoSeTipa("Ana");

        entoncesElResultadoEs("Ana");
    }

    @Test
    @DisplayName("En el reporte el valor va como texto")
    void enElReporteVaComoTexto() {
        cuandoSePideLaCeldaDeReporte("Ana");

        entoncesElResultadoEs("Ana");
    }
}
