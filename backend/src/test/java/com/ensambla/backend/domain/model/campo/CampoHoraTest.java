package com.ensambla.backend.domain.model.campo;

class CampoHoraTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.HORA;
    }
}
