package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import org.springframework.core.io.Resource;

/**
 * Arma el pedido a la IA para pasar lo que escribe un cliente a una lista de cosas con su cantidad, a partir de
 * {@code ia/prompt-whatsapp-pedido.md} y {@code ia/esquema-whatsapp-pedido.json}.
 */
public class PromptWhatsappPedido {

    private final String instrucciones;
    private final String esquemaJson;

    public PromptWhatsappPedido(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    public PeticionModeloTexto pedido(ConsultaPedido consulta) {
        StringBuilder contexto = new StringBuilder();
        if (!consulta.productos().isEmpty()) {
            contexto.append("Productos del comercio:\n");
            consulta.productos().forEach(producto -> contexto.append("- ").append(producto).append('\n'));
            contexto.append('\n');
        }
        contexto.append("Mensaje del cliente:\n\"\"\"\n").append(consulta.texto()).append("\n\"\"\"\n");
        return new PeticionModeloTexto(instrucciones, contexto.toString(), esquemaJson);
    }
}
