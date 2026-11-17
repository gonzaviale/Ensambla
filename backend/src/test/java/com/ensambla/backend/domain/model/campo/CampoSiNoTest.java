package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class CampoSiNoTest extends CampoDeCualquierTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.SI_NO;
    }

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of(true, false);
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of("sí", 1);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Tiene que ser sí o no";
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "SI", "sí"})
    @DisplayName("Las distintas formas de decir sí se entienden como sí")
    void lasFormasDeDecirSiSeEntiendenComoSi(String respuesta) {
        cuandoSeTipa(respuesta);

        entoncesElResultadoEs(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "No"})
    @DisplayName("Las distintas formas de decir no se entienden como no")
    void lasFormasDeDecirNoSeEntiendenComoNo(String respuesta) {
        cuandoSeTipa(respuesta);

        entoncesElResultadoEs(false);
    }

    @Test
    @DisplayName("Lo que no es ni sí ni no queda como texto")
    void loQueNoEsNiSiNiNoQuedaComoTexto() {
        cuandoSeTipa("quizás");

        entoncesElResultadoEs("quizás");
    }

    @Test
    @DisplayName("En el reporte un sí se escribe «Sí»")
    void enElReporteUnSiSeEscribeSi() {
        cuandoSePideLaCeldaDeReporte(true);

        entoncesElResultadoEs("Sí");
    }

    @Test
    @DisplayName("En el reporte un no se escribe «No»")
    void enElReporteUnNoSeEscribeNo() {
        cuandoSePideLaCeldaDeReporte(false);

        entoncesElResultadoEs("No");
    }

    @Override
    protected List<String> textosLeidosQueAcepta() {
        return List.of("true", "false");
    }

    @Override
    protected List<String> textosLeidosQueRechaza() {
        return List.of("sí", "TRUE");
    }
}
