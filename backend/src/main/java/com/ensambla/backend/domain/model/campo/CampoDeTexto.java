package com.ensambla.backend.domain.model.campo;

import java.util.Optional;

/** Campo que guarda un texto de hasta {@value #MAX_CARACTERES} caracteres. */
public abstract class CampoDeTexto extends Campo {

    static final int MAX_CARACTERES = 1000;

    protected CampoDeTexto(CampoBuilder datos) {
        super(datos);
    }

    @Override
    public Optional<String> errorDe(Object valor) {
        if (!(valor instanceof String texto)) {
            return Optional.of("Tiene que ser un texto");
        }
        return errorDeLargo(texto);
    }

    @Override
    protected Optional<String> errorDeValorInicial(String valor) {
        return errorDeLargo(valor);
    }

    private Optional<String> errorDeLargo(String texto) {
        return errorSi(texto.length() > MAX_CARACTERES, "Máximo " + MAX_CARACTERES + " caracteres");
    }
}
