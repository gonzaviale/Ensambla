package com.ensambla.backend.domain.model.campo;

public class CampoTelefono extends CampoDeTexto {

    public CampoTelefono(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.TELEFONO;
    }
}
