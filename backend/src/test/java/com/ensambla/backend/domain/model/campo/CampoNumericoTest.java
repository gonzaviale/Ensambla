package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

abstract class CampoNumericoTest extends CampoDeCualquierTipoTest {

    private List<Campo> camposDelBloque;
    private Throwable rechazo;

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of(12, 12.5);
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of("12", Double.POSITIVE_INFINITY, Double.NaN);
    }

    @Override
    protected String motivoDelRechazo() {
        return "Tiene que ser un número";
    }

    @Test
    @DisplayName("Es un campo numérico")
    void esUnCampoNumerico() {
        entoncesElCampoEsNumerico();
    }

    @Test
    @DisplayName("Un número escrito con coma o con espacios se entiende")
    void unNumeroEscritoConComaOConEspaciosSeEntiende() {
        cuandoSeTipa("1 200,5");

        entoncesElResultadoEs(new BigDecimal("1200.5"));
    }

    @Test
    @DisplayName("Lo que no es un número queda como texto")
    void loQueNoEsUnNumeroQuedaComoTexto() {
        cuandoSeTipa("mucho");

        entoncesElResultadoEs("mucho");
    }

    @Test
    @DisplayName("En el reporte el valor va como número")
    void enElReporteVaComoNumero() {
        cuandoSePideLaCeldaDeReporte("12.50");

        entoncesElResultadoEs(new BigDecimal("12.50"));
    }

    protected void dadoUnBloqueCon(Campo... campos) {
        camposDelBloque = List.of(campos);
    }

    protected void cuandoSeValidaElCambioDeTipoDe(Campo campo) {
        rechazo = catchThrowable(() -> campo.validarCambioDeTipo(camposDelBloque));
    }

    protected void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).hasMessage(motivo);
    }

    protected void entoncesNoSeRechazaNada() {
        assertThat(rechazo).isNull();
    }

    protected Campo unPrecioNumerico() {
        return unCampoNumerico("Precio");
    }

    private void entoncesElCampoEsNumerico() {
        assertThat(unCampo().esNumerico()).isTrue();
    }

    @Override
    protected List<String> textosLeidosQueAcepta() {
        return List.of("12.5", "1e5", "-3");
    }

    @Override
    protected List<String> textosLeidosQueRechaza() {
        return List.of("mucho", "12,5");
    }

    @Override
    protected Optional<ClaseDeDatoInterpretable> claseInterpretableEsperada() {
        return Optional.of(ClaseDeDatoInterpretable.NUMERO);
    }
}
