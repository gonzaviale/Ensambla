package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CampoTest {

    @Test
    @DisplayName("Un campo marcado como obligatorio es obligatorio")
    void esObligatorioShouldBeTrueWhenMarked() {
        Campo campo = Campo.builder().obligatorio(true).build();

        assertThat(campo.esObligatorio()).isTrue();
    }

    @Test
    @DisplayName("Un campo sin marcar o sin dato no es obligatorio")
    void esObligatorioShouldBeFalseWhenNotMarkedOrNull() {
        Campo sinMarcar = Campo.builder().obligatorio(false).build();
        Campo sinDato = Campo.builder().build();

        assertThat(sinMarcar.esObligatorio()).isFalse();
        assertThat(sinDato.esObligatorio()).isFalse();
    }

    @Test
    @DisplayName("Acepta un texto de hasta 1000 caracteres")
    void errorDeShouldBeEmptyWhenTextoIsValid() {
        assertThat(errorDe(TipoCampo.TEXTO, "x".repeat(1000))).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto que supera los 1000 caracteres")
    void errorDeShouldReportWhenTextoIsTooLong() {
        assertThat(errorDe(TipoCampo.TEXTO, "x".repeat(1001))).contains("Máximo 1000 caracteres");
    }

    @Test
    @DisplayName("Rechaza un número cargado donde se espera un texto")
    void errorDeShouldReportWhenTextoIsNotAString() {
        assertThat(errorDe(TipoCampo.TEXTO, 12)).contains("Tiene que ser un texto");
    }

    @Test
    @DisplayName("Acepta enteros y decimales como número")
    void errorDeShouldBeEmptyWhenNumeroIsNumber() {
        assertThat(errorDe(TipoCampo.NUMERO, 12)).isEmpty();
        assertThat(errorDe(TipoCampo.NUMERO, 12.5)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto donde se espera un número")
    void errorDeShouldReportWhenNumeroIsString() {
        assertThat(errorDe(TipoCampo.NUMERO, "12")).contains("Tiene que ser un número");
    }

    @Test
    @DisplayName("Rechaza un número infinito")
    void errorDeShouldReportWhenNumeroIsNotFinite() {
        assertThat(errorDe(TipoCampo.NUMERO, Double.POSITIVE_INFINITY)).contains("Tiene que ser un número");
    }

    @Test
    @DisplayName("Acepta una fecha en formato AAAA-MM-DD")
    void errorDeShouldBeEmptyWhenFechaIsIso() {
        assertThat(errorDe(TipoCampo.FECHA, "2026-10-09")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una fecha que no existe en el calendario")
    void errorDeShouldReportWhenFechaDoesNotExist() {
        assertThat(errorDe(TipoCampo.FECHA, "2026-02-31")).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Rechaza una fecha con otro formato")
    void errorDeShouldReportWhenFechaHasOtherFormat() {
        assertThat(errorDe(TipoCampo.FECHA, "09/10/2026")).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Rechaza una fecha que no es un texto")
    void errorDeShouldReportWhenFechaIsNotAString() {
        assertThat(errorDe(TipoCampo.FECHA, 20261009)).contains("Tiene que ser una fecha válida");
    }

    @Test
    @DisplayName("Acepta una de las opciones del campo")
    void errorDeShouldBeEmptyWhenOpcionIsListed() {
        assertThat(errorDe(TipoCampo.OPCION, "Mayorista")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una opción que no está en la lista")
    void errorDeShouldReportWhenOpcionIsNotListed() {
        assertThat(errorDe(TipoCampo.OPCION, "Otra")).contains("Elegí una de las opciones");
    }

    @Test
    @DisplayName("Rechaza una opción que no es un texto")
    void errorDeShouldReportWhenOpcionIsNotAString() {
        assertThat(errorDe(TipoCampo.OPCION, true)).contains("Elegí una de las opciones");
    }

    @Test
    @DisplayName("Acepta verdadero y falso como sí o no")
    void errorDeShouldBeEmptyWhenSiNoIsBoolean() {
        assertThat(errorDe(TipoCampo.SI_NO, false)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un texto donde se espera sí o no")
    void errorDeShouldReportWhenSiNoIsString() {
        assertThat(errorDe(TipoCampo.SI_NO, "sí")).contains("Tiene que ser sí o no");
    }

    // --- act ---
    private Optional<String> errorDe(TipoCampo tipo, Object valor) {
        return campo(tipo).errorDe(valor);
    }

    // --- helpers ---
    private Campo campo(TipoCampo tipo) {
        return Campo.builder()
                .tipo(tipo)
                .opciones(List.of("Minorista", "Mayorista"))
                .build();
    }
}
