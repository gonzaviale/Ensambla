package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Arma el pedido a la IA para redactar un cartel, a partir de {@code ia/prompt-carteles.md} y
 * {@code ia/esquema-carteles.json}. Los nombres que escribió la persona van entre «» para que el modelo los tome como
 * datos, y no se manda nada de clientes, deudas, ventas ni de la dirección del negocio.
 */
public class PromptCarteles {

    private final String instrucciones;
    private final String esquemaJson;

    public PromptCarteles(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    public PeticionModeloTexto peticion(PedidoDeTextosDeCartel pedido) {
        String contexto = String.join("\n",
                "Negocio: «" + pedido.negocio() + "» (rubro: " + pedido.rubro().getNombre() + ")",
                "Tono: " + pedido.tono().indicacion(),
                "Plantillas posibles: " + pedido.datos().tiposPosibles().stream()
                        .map(Enum::name).collect(Collectors.joining(", ")),
                "Productos: " + entreComillas(pedido.datos().productos()),
                "Números permitidos: " + numeros(pedido.datos().numeros()));
        return new PeticionModeloTexto(instrucciones, contexto, esquemaJson);
    }

    private String entreComillas(List<String> nombres) {
        return nombres.stream().map(nombre -> "«" + nombre + "»").collect(Collectors.joining(", "));
    }

    private String numeros(List<BigDecimal> numeros) {
        return numeros.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(", "));
    }
}
