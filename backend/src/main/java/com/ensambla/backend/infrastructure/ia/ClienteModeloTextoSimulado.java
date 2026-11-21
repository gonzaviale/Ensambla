package com.ensambla.backend.infrastructure.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente sin costo para desarrollar y probar: elige la primera plantilla de la lista con confianza media.
 * No interpreta el texto; sólo prueba el circuito (consumo, límites, validación). Es el que usa la conversación de un
 * objetivo cuando el proveedor es el simulado.
 */
public class ClienteModeloTextoSimulado implements ClienteModeloTexto {

    static final String PROVEEDOR = "simulado";
    static final Pattern PRIMERA_PLANTILLA = Pattern.compile("(?m)^- ([^:\\n]+): ");
    static final Pattern PRIMERA_METRICA = Pattern.compile("(?m)^(c\\d+): ");
    static final Pattern CADA_RENOVACION = Pattern.compile("(?m)^(r\\d+): ");

    @Override
    public RespuestaModeloTexto completar(PeticionModeloTexto peticion) {
        if (peticion.contexto().contains(PromptConversacionObjetivos.CATALOGO)) {
            return conversacionDeObjetivo(peticion);
        }
        if (peticion.contexto().startsWith(PromptRenovacionManuales.RENOVACIONES)) {
            return renovacionesDeManuales(peticion);
        }
        Matcher plantilla = PRIMERA_PLANTILLA.matcher(peticion.contexto());
        String nombre = plantilla.find() ? plantilla.group(1) : "";
        String json = "{\"plantilla\":\"%s\",\"columnas\":[],\"confianza\":0.5}".formatted(nombre);
        return new RespuestaModeloTexto(json, PROVEEDOR, PROVEEDOR, 0, 0);
    }

    /** Una renovación genérica por cada código del pedido; sólo prueba el circuito. */
    private static RespuestaModeloTexto renovacionesDeManuales(PeticionModeloTexto peticion) {
        Matcher renovacion = CADA_RENOVACION.matcher(peticion.contexto());
        List<String> minis = new ArrayList<>();
        while (renovacion.find()) {
            minis.add(("{\"codigo\":\"%s\",\"titulo\":\"Seguí con tu objetivo esta semana\","
                    + "\"justificacion\":\"Contale a Sami cómo te fue con esta acción.\"}")
                    .formatted(renovacion.group(1)));
        }
        return new RespuestaModeloTexto("{\"minis\":[" + String.join(",", minis) + "]}", PROVEEDOR, PROVEEDOR, 0, 0);
    }

    /**
     * La conversación de un objetivo: elige la primera métrica del catálogo (si hay) y suma una acción manual. No
     * interpreta lo que escribió la persona; sólo prueba el circuito.
     */
    private static RespuestaModeloTexto conversacionDeObjetivo(PeticionModeloTexto peticion) {
        Matcher metrica = PRIMERA_METRICA.matcher(peticion.contexto());
        String medido = metrica.find() ? "{\"modo\":\"MEDIDO\",\"codigo\":\"%s\"},".formatted(metrica.group(1)) : "";
        String json = ("{\"respuesta\":\"Armé una propuesta con lo que sé de tu negocio. Revisala antes de guardar.\","
                + "\"entendido\":\"Tu objetivo, tal como me lo contaste.\",\"esMonetario\":false,\"minis\":[%s"
                + "{\"modo\":\"MANUAL\",\"titulo\":\"Anotá cómo te fue cada semana\","
                + "\"justificacion\":\"Así vemos juntos qué funciona y qué no.\"}]}").formatted(medido);
        return new RespuestaModeloTexto(json, PROVEEDOR, PROVEEDOR, 0, 0);
    }
}
