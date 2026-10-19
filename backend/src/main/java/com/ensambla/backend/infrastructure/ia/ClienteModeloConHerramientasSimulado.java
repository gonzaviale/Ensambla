package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;

import java.util.List;
import java.util.Map;

/**
 * Cliente sin costo para desarrollar y probar el taller. No interpreta nada: ante «gasto» suma la pieza de gastos y
 * en cualquier otro caso pregunta qué hacer con una botonera. Sirve para ver el circuito completo sin llamar a un
 * proveedor.
 */
public class ClienteModeloConHerramientasSimulado implements ClienteModeloConHerramientas {

    static final String PROVEEDOR = "simulado";

    @Override
    public RespuestaConHerramientas completar(PeticionConHerramientas peticion) {
        String contexto = peticion.contexto();
        int inicio = contexto.lastIndexOf(PromptTaller.MARCA_MENSAJE);
        String mensaje = inicio < 0 ? contexto : contexto.substring(inicio);
        if (mensaje.toLowerCase().contains("gasto")) {
            return respuesta("Listo, sumé los gastos.", List.of(
                    new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))));
        }
        return respuesta("Te cuento qué puedo hacer. ¿Por dónde seguimos?", List.of(
                new LlamadaHerramienta("ofrecer_opciones", Map.of(
                        "pregunta", "¿Qué querés hacer con tu sistema?",
                        "opciones", List.of("Sumar gastos", "Separar los clientes", "Cambiar la distribución")))));
    }

    private RespuestaConHerramientas respuesta(String texto, List<LlamadaHerramienta> llamadas) {
        return new RespuestaConHerramientas(texto, llamadas, PROVEEDOR, PROVEEDOR, 0, 0);
    }
}
