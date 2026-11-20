package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.Map;

/**
 * Una acción guardada con datos incompletos: al aplicarla se informa qué le falta. Conserva los datos tal como
 * estaban guardados para no perderlos.
 */
public record AccionInvalida(String tipo, String motivo, Map<String, Object> datos) implements AccionDeAviso {

    @Override
    public boolean modificaDatos() {
        return true;
    }

    @Override
    public void aplicarEn(AplicadorDeAcciones aplicador) {
        throw new BusinessRuleException(motivo);
    }
}
