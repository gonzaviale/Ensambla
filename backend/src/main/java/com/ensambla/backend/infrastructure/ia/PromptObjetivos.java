package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma el pedido a la IA para redactar mini-objetivos, a partir de {@code ia/prompt-objetivos.md} y
 * {@code ia/esquema-objetivos.json}. Le habla códigos cortos (m1, m2...) y le da los números ya calculados, con el
 * texto de reserva como ejemplo del tono.
 */
public class PromptObjetivos {

    private final String instrucciones;
    private final String esquemaJson;

    public PromptObjetivos(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    /** El código de cada candidato es su lugar en la lista: m1 es el primero. */
    public static String codigo(int indice) {
        return "m" + (indice + 1);
    }

    public PeticionModeloTexto peticion(List<CandidatoMiniObjetivo> candidatos) {
        List<String> lineas = new ArrayList<>();
        for (int indice = 0; indice < candidatos.size(); indice++) {
            CandidatoMiniObjetivo candidato = candidatos.get(indice);
            lineas.add(("%s: tipo %s, sobre %s. Hoy vale %s y la meta es %s. Suma cerca de $%s por mes. "
                    + "Texto de reserva: ")
                    .formatted(codigo(indice), candidato.tipo(),
                            candidato.referencia() == null ? "el negocio" : candidato.referencia(),
                            candidato.valorInicial().toPlainString(), candidato.meta().toPlainString(),
                            candidato.aporteEstimado().toPlainString())
                    + candidato.titulo() + ". " + candidato.justificacion());
        }
        return new PeticionModeloTexto(instrucciones,
                "Mini-objetivos a redactar:\n" + String.join("\n", lineas), esquemaJson);
    }
}
