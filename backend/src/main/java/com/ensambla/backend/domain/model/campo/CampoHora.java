package com.ensambla.backend.domain.model.campo;

public class CampoHora extends CampoDeTexto {

    public CampoHora(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.HORA;
    }
}
