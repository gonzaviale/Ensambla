package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.CamposDePrueba.unCampoDeMoneda;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;

class CampoMonedaTest extends CampoNumericoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.MONEDA;
    }

    @Test
    @DisplayName("Una moneda no puede cambiar de tipo si una fórmula la usa")
    void unaMonedaNoPuedeCambiarDeTipoSiUnaFormulaLaUsa() {
        Campo precio = unCampoDeMoneda("Precio");
        dadoUnBloqueCon(precio, unaFormulaQueUsa(precio, "Total"));

        cuandoSeValidaElCambioDeTipoDe(precio);

        entoncesSeRechazaPorque("No se puede cambiar el tipo de «Precio»: la fórmula «Total» lo usa");
    }
}
