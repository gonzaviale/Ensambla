package com.ensambla.backend.infrastructure.ia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente sin costo para desarrollar y probar: elige la primera plantilla de la lista con confianza media.
 * No interpreta el texto; sólo prueba el circuito (consumo, límites, validación).
 */
public class ClienteModeloTextoSimulado implements ClienteModeloTexto {

    static final String PROVEEDOR = "simulado";
    static final Pattern PRIMERA_PLANTILLA = Pattern.compile("(?m)^- ([^:\\n]+): ");

    @Override
    public RespuestaModeloTexto completar(PeticionModeloTexto peticion) {
        Matcher plantilla = PRIMERA_PLANTILLA.matcher(peticion.contexto());
        String nombre = plantilla.find() ? plantilla.group(1) : "";
        String json = "{\"plantilla\":\"%s\",\"columnas\":[],\"confianza\":0.5}".formatted(nombre);
        return new RespuestaModeloTexto(json, PROVEEDOR, PROVEEDOR, 0, 0);
    }
}
