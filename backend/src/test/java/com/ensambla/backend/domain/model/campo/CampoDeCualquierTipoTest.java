package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTipo;
import static org.assertj.core.api.Assertions.assertThat;

/** Lo que tiene que cumplir todo tipo de campo; cada subclase aporta solo sus valores y su motivo de rechazo. */
abstract class CampoDeCualquierTipoTest {

    private Optional<String> error;
    private Object resultado;

    protected abstract TipoCampo tipoEsperado();

    protected abstract List<Object> valoresQueAcepta();

    protected abstract List<Object> valoresQueRechaza();

    protected abstract String motivoDelRechazo();

    protected List<String> textosLeidosQueAcepta() {
        return List.of("cualquier texto");
    }

    protected List<String> textosLeidosQueRechaza() {
        return List.of();
    }

    protected Optional<ClaseDeDatoInterpretable> claseInterpretableEsperada() {
        return Optional.empty();
    }

    protected Campo unCampo() {
        return unCampoDeTipo(tipoEsperado());
    }

    @Test
    @DisplayName("El campo es del tipo que le corresponde")
    void elCampoEsDelTipoEsperado() {
        entoncesElCampoEsDelTipoEsperado();
    }

    @Test
    @DisplayName("Acepta los valores que son de su tipo")
    void aceptaLosValoresDeSuTipo() {
        entoncesNingunValorTieneError(valoresQueAcepta());
    }

    @Test
    @DisplayName("Rechaza los valores que no son de su tipo con el motivo correspondiente")
    void rechazaLosValoresQueNoSonDeSuTipo() {
        entoncesTodosLosValoresSeRechazan(valoresQueRechaza());
    }

    @Test
    @DisplayName("Acepta los textos que lee la IA y que sirven para su tipo")
    void aceptaLosTextosQueLeeLaIaDeSuTipo() {
        entoncesNingunTextoLeidoTieneError(textosLeidosQueAcepta());
    }

    @Test
    @DisplayName("Rechaza los textos que lee la IA y que no sirven para su tipo")
    void rechazaLosTextosQueLeeLaIaQueNoSonDeSuTipo() {
        entoncesTodosLosTextosLeidosSeRechazan(textosLeidosQueRechaza());
    }

    @Test
    @DisplayName("Sabe qué clase de dato de un mensaje libre lo completa")
    void sabeQueClaseDeDatoDeUnMensajeLoCompleta() {
        entoncesSuClaseInterpretableEs(claseInterpretableEsperada());
    }

    protected void cuandoSeValida(Object valor) {
        error = unCampo().errorDe(valor);
    }

    protected void cuandoSeTipa(String texto) {
        resultado = unCampo().tipar(texto);
    }

    protected void cuandoSePideLaCeldaDeReporte(Object valor) {
        resultado = unCampo().celdaDeReporte(valor, id -> "otra fila");
    }

    protected void cuandoSePideLaCeldaDeReporte(Object valor, Function<RegistroId, String> etiquetas) {
        resultado = unCampo().celdaDeReporte(valor, etiquetas);
    }

    protected void entoncesElErrorEs(String mensaje) {
        assertThat(error).contains(mensaje);
    }

    protected void entoncesElResultadoEs(Object esperado) {
        assertThat(resultado).isEqualTo(esperado);
    }

    protected void entoncesElCampoEsDelTipoEsperado() {
        assertThat(unCampo().tipo()).isEqualTo(tipoEsperado());
        assertThat(unCampo().getTipo()).isEqualTo(tipoEsperado());
    }

    private void entoncesNingunTextoLeidoTieneError(List<String> textos) {
        assertThat(textos).allSatisfy(texto -> assertThat(unCampo().errorDeValorLeido(texto)).isEmpty());
    }

    private void entoncesTodosLosTextosLeidosSeRechazan(List<String> textos) {
        assertThat(textos).allSatisfy(texto -> assertThat(unCampo().errorDeValorLeido(texto)).isPresent());
    }

    private void entoncesSuClaseInterpretableEs(Optional<ClaseDeDatoInterpretable> esperada) {
        assertThat(unCampo().claseInterpretable()).isEqualTo(esperada);
    }

    private void entoncesNingunValorTieneError(List<Object> valores) {
        assertThat(valores).allSatisfy(valor -> assertThat(unCampo().errorDe(valor)).isEmpty());
    }

    private void entoncesTodosLosValoresSeRechazan(List<Object> valores) {
        assertThat(valores)
                .allSatisfy(valor -> assertThat(unCampo().errorDe(valor)).contains(motivoDelRechazo()));
    }
}
