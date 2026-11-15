package com.ensambla.backend.domain.model.campo;

class CampoEmailTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.EMAIL;
    }
}
