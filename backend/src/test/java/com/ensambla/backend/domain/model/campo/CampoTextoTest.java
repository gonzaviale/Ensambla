package com.ensambla.backend.domain.model.campo;

import java.util.Optional;

class CampoTextoTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.TEXTO;
    }

    @Override
    protected Optional<ClaseDeDatoInterpretable> claseInterpretableEsperada() {
        return Optional.of(ClaseDeDatoInterpretable.TEXTO);
    }
}
