package com.ensambla.backend.domain.model.campo;

public class CampoTexto extends CampoDeTexto {

    public CampoTexto(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.TEXTO;
    }
}
