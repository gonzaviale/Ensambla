package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import org.springframework.core.io.Resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el pedido a la IA para pasar un texto a filas, a partir de {@code ia/prompt-whatsapp-registros.md} y
 * {@code ia/esquema-whatsapp-registros.json}. A la IA se le hablan códigos cortos (t1, c1...) en lugar de ids: así
 * no puede inventar uno y la respuesta se traduce de vuelta sin ambigüedad.
 */
public class PromptWhatsappRegistros {

    /** El pedido y cómo traducir los códigos de la respuesta a los ids de las tablas y los campos. */
    public record Pedido(PeticionModeloTexto peticion, Map<String, BloqueId> tablas, Map<String, CampoId> campos,
                         Map<CampoId, BloqueId> tablaDeCadaCampo) {
    }

    private final String instrucciones;
    private final String esquemaJson;

    public PromptWhatsappRegistros(Resource instrucciones, Resource esquemaJson) {
        this.instrucciones = RecursoTexto.contenido(instrucciones);
        this.esquemaJson = RecursoTexto.contenido(esquemaJson);
    }

    public Pedido pedido(ConsultaInterpretacion consulta) {
        Map<String, BloqueId> tablas = new LinkedHashMap<>();
        Map<String, CampoId> campos = new LinkedHashMap<>();
        Map<CampoId, BloqueId> tablaDeCadaCampo = new LinkedHashMap<>();
        StringBuilder contexto = new StringBuilder("Hoy es %s.\n\nTablas del negocio:\n".formatted(consulta.hoy()));
        String elegida = null;
        for (TablaDisponible tabla : consulta.tablas()) {
            String codigoTabla = "t" + (tablas.size() + 1);
            tablas.put(codigoTabla, tabla.bloque().id());
            if (tabla.bloque().id().equals(consulta.destino())) {
                elegida = codigoTabla;
            }
            contexto.append("%s: %s\n".formatted(codigoTabla, tabla.etiqueta()));
            for (CampoConocido campo : cargables(tabla)) {
                String codigoCampo = "c" + (campos.size() + 1);
                campos.put(codigoCampo, campo.id());
                tablaDeCadaCampo.put(campo.id(), tabla.bloque().id());
                contexto.append("  %s %s (%s)%s\n".formatted(codigoCampo, campo.nombre(),
                        tipo(campo, consulta.tablas()), opciones(campo.opciones())));
            }
            List<String> nombres = consulta.nombres().getOrDefault(tabla.bloque().id(), List.of());
            if (!nombres.isEmpty()) {
                contexto.append("  ya cargados: ").append(String.join("; ", nombres)).append('\n');
            }
        }
        if (elegida != null) {
            contexto.append("\nTabla elegida: ").append(elegida).append('\n');
        }
        contexto.append("\nMensaje de la persona:\n\"\"\"\n").append(consulta.texto()).append("\n\"\"\"\n");
        return new Pedido(new PeticionModeloTexto(instrucciones, contexto.toString(), esquemaJson),
                tablas, campos, tablaDeCadaCampo);
    }

    /** Las columnas que se pueden cargar a mano: no las calculadas. */
    private static List<CampoConocido> cargables(TablaDisponible tabla) {
        return tabla.bloque().campos().stream().filter(campo -> campo.tipo() != TipoCampo.FORMULA).toList();
    }

    /** Un vínculo dice a qué tabla apunta, así la IA escribe el nombre de una de sus filas. */
    private static String tipo(CampoConocido campo, List<TablaDisponible> tablas) {
        if (campo.tipo() != TipoCampo.RELACION) {
            return campo.tipo().name();
        }
        return tablas.stream()
                .filter(tabla -> tabla.bloque().id().equals(campo.bloqueRelacionadoId()))
                .findFirst()
                .map(tabla -> "RELACION a " + tabla.bloque().nombre())
                .orElse(TipoCampo.RELACION.name());
    }

    private static String opciones(List<String> opciones) {
        return opciones.isEmpty() ? "" : " opciones: " + String.join(", ", opciones);
    }
}
