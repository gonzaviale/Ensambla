package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;

class CampoNumeroTest extends CampoNumericoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.NUMERO;
    }

    @Test
    @DisplayName("Un número puede seguir siendo número aunque una fórmula lo use")
    void unNumeroPuedeSeguirSiendoNumeroAunqueUnaFormulaLoUse() {
        Campo precio = unPrecioNumerico();
        dadoUnBloqueCon(precio, unaFormulaQueUsa(precio, "Total"));

        cuandoSeValidaElCambioDeTipoDe(precio);

        entoncesNoSeRechazaNada();
    }
}
