package com.ensambla.backend.infrastructure.presentation.dto.zona;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizarZonaWebRequestTest {

    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");
    private static final String MENSAJE = "Indicá un lugar o un punto del mapa";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<AnalizarZonaWebRequest>> violaciones;

    @Test
    @DisplayName("Es válido con sólo un lugar escrito")
    void esValidoConSoloUnLugarEscrito() {
        cuandoSeValida(new AnalizarZonaWebRequest("San Justo", null, null));

        entoncesNoHayViolaciones();
    }

    @Test
    @DisplayName("Es válido con sólo un punto del mapa")
    void esValidoConSoloUnPuntoDelMapa() {
        cuandoSeValida(new AnalizarZonaWebRequest(null, LATITUD, LONGITUD));

        entoncesNoHayViolaciones();
    }

    @Test
    @DisplayName("Rechaza un pedido sin lugar ni punto")
    void rechazaUnPedidoSinLugarNiPunto() {
        cuandoSeValida(new AnalizarZonaWebRequest(null, null, null));

        entoncesSePideIndicarUnLugarOUnPunto();
    }

    @Test
    @DisplayName("Rechaza un lugar en blanco sin punto")
    void rechazaUnLugarEnBlancoSinPunto() {
        cuandoSeValida(new AnalizarZonaWebRequest("   ", null, null));

        entoncesSePideIndicarUnLugarOUnPunto();
    }

    @Test
    @DisplayName("Rechaza un punto al que le falta la longitud")
    void rechazaUnPuntoAlQueLeFaltaLaLongitud() {
        cuandoSeValida(new AnalizarZonaWebRequest(null, LATITUD, null));

        entoncesSePideIndicarUnLugarOUnPunto();
    }

    @Test
    @DisplayName("Rechaza una latitud fuera de rango")
    void rechazaUnaLatitudFueraDeRango() {
        cuandoSeValida(new AnalizarZonaWebRequest(null, new BigDecimal("91"), LONGITUD));

        entoncesSeRechazaElCampo("latitud");
    }

    @Test
    @DisplayName("Rechaza una longitud fuera de rango")
    void rechazaUnaLongitudFueraDeRango() {
        cuandoSeValida(new AnalizarZonaWebRequest(null, LATITUD, new BigDecimal("-181")));

        entoncesSeRechazaElCampo("longitud");
    }

    // --- cuando ---

    private void cuandoSeValida(AnalizarZonaWebRequest pedido) {
        violaciones = validator.validate(pedido);
    }

    // --- entonces ---

    private void entoncesNoHayViolaciones() {
        assertThat(violaciones).isEmpty();
    }

    private void entoncesSePideIndicarUnLugarOUnPunto() {
        assertThat(violaciones).extracting(ConstraintViolation::getMessage).containsExactly(MENSAJE);
    }

    private void entoncesSeRechazaElCampo(String campo) {
        assertThat(violaciones).extracting(violacion -> violacion.getPropertyPath().toString())
                .containsExactly(campo);
    }
}
