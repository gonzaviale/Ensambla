package com.ensambla.backend.domain.model.campo;

class CampoTelefonoTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.TELEFONO;
    }
}
