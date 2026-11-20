package com.ensambla.backend.domain.model.campo;

import java.util.List;

public class CampoNumero extends CampoNumerico {

    public CampoNumero(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.NUMERO;
    }

    @Override
    public boolean sumaEnElReporte() {
        return true;
    }

    /** Un número puede seguir siendo número aunque una fórmula lo use. */
    @Override
    public void validarCambioDeTipo(List<Campo> camposDelBloque) {
    }
}
