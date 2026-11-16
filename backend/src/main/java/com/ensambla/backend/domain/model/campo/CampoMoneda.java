package com.ensambla.backend.domain.model.campo;

public class CampoMoneda extends CampoNumerico {

    public CampoMoneda(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.MONEDA;
    }
}
