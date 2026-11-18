package com.ensambla.backend.domain.model.campo;

import java.util.Optional;

public class CampoTexto extends CampoDeTexto {

    public CampoTexto(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public Optional<ClaseDeDatoInterpretable> claseInterpretable() {
        return Optional.of(ClaseDeDatoInterpretable.TEXTO);
    }

    @Override
    public TipoCampo tipo() {
        return TipoCampo.TEXTO;
    }
}
