package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma el pedido a la IA para redactar la acción que renueva cada mini-objetivo manual, a partir de
 * {@code ia/prompt-renovacion-manuales.md} y {@code ia/esquema-objetivos.json}. Le habla códigos cortos (r1, r2…); lo
 * que escribió la persona va entre marcas como dato.
 */
public class PromptRenovacionManuales {

    /** Encabezado de los pedidos: lo reconoce también el cliente simulado. */
    static final String RENOVACIONES = "Renovaciones a redactar:";

    private final String instrucciones;
    private final String esquemaJson;

    public PromptRenovacionManuales(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    /** El código de cada pedido es su lugar en la lista: r1 es el primero. */
    public static String codigo(int indice) {
        return "r" + (indice + 1);
    }

    public PeticionModeloTexto peticion(List<PedidoRenovacion> pedidos) {
        List<String> lineas = new ArrayList<>();
        lineas.add(RENOVACIONES);
        for (int indice = 0; indice < pedidos.size(); indice++) {
            PedidoRenovacion pedido = pedidos.get(indice);
            lineas.add("%s: qué hacer: %s. Hizo: %s. Qué pasó: %s. Acción anterior: «%s» (%s). Lo que contó: %s"
                    .formatted(codigo(indice), pedido.accion(), pedido.hizo(), pedido.quePaso(), pedido.titulo(),
                            pedido.justificacion(),
                            pedido.comentario() == null ? "nada" : "<<<" + pedido.comentario() + ">>>"));
        }
        return new PeticionModeloTexto(instrucciones, String.join("\n", lineas), esquemaJson);
    }
}
