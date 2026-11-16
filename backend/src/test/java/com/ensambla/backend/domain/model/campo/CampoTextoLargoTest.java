package com.ensambla.backend.domain.model.campo;

class CampoTextoLargoTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.TEXTO_LARGO;
    }
}
