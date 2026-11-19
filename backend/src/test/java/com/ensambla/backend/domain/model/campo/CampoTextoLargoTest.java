package com.ensambla.backend.domain.model.campo;

import java.util.Optional;

class CampoTextoLargoTest extends CampoDeTextoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.TEXTO_LARGO;
    }

    @Override
    protected Optional<ClaseDeDatoInterpretable> claseInterpretableEsperada() {
        return Optional.of(ClaseDeDatoInterpretable.TEXTO);
    }
}
