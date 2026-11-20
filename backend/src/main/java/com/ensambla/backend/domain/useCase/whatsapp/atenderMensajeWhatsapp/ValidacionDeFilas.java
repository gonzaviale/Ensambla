package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valida los valores de una fila contra los campos de su tabla y, si algo está mal, lo explica con los nombres de la persona. */
final class ValidacionDeFilas {

    private ValidacionDeFilas() {
    }

    static Map<String, Object> validar(List<Campo> campos, Map<String, Object> valores) {
        try {
            return Registro.validar(campos, valores);
        } catch (RegistroInvalidoException e) {
            throw new BusinessRuleException(TextosWhatsapp.datosInvalidos(e.getErrores(), nombresPorCampo(campos)));
        }
    }

    private static Map<String, String> nombresPorCampo(List<Campo> campos) {
        Map<String, String> nombres = new LinkedHashMap<>();
        campos.forEach(campo -> nombres.put(campo.getId().toString(), campo.getNombre()));
        return nombres;
    }
}
