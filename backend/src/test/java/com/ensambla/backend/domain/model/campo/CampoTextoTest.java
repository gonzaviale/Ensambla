package com.ensambla.backend.domain.model.campo;

class CampoTextoTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.TEXTO;
    }
}
