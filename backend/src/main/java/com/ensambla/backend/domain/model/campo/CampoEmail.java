package com.ensambla.backend.domain.model.campo;

public class CampoEmail extends CampoDeTexto {

    public CampoEmail(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.EMAIL;
    }
}
