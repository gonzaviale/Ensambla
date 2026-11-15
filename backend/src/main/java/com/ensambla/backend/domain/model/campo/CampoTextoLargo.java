package com.ensambla.backend.domain.model.campo;

public class CampoTextoLargo extends CampoDeTexto {

    public CampoTextoLargo(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.TEXTO_LARGO;
    }
}
