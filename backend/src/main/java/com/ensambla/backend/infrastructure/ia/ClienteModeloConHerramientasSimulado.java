package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cliente sin costo para desarrollar y probar el taller; no interpreta nada. Ante «gasto» suma la pieza de gastos. Con
 * piezas ya armadas pregunta qué ajustar. Con el sistema vacío, si el contexto trae el kit del rubro lo arma
 * ({@code usar_kit}) y hace la pregunta del kit; si no reconoció el rubro, lo pregunta ofreciendo los primeros kits.
 * Sirve para ver el circuito completo sin llamar a un proveedor.
 */
public class ClienteModeloConHerramientasSimulado implements ClienteModeloConHerramientas {

    static final String PROVEEDOR = "simulado";
    static final String PREGUNTA_RUBRO = "¿De qué es tu negocio?";
    static final String PREGUNTA_AJUSTE = "¿Qué querés ajustar?";
    static final List<String> OPCIONES_AJUSTE = List.of("Sumar gastos", "Así está bien");
    static final int RUBROS_OFRECIDOS = 4;

    private final KitsRubro kitsRubro;

    public ClienteModeloConHerramientasSimulado(KitsRubro kitsRubro) {
        this.kitsRubro = kitsRubro;
    }

    @Override
    public RespuestaConHerramientas completar(PeticionConHerramientas peticion) {
        String contexto = peticion.contexto();
        int inicio = contexto.lastIndexOf(PromptTaller.MARCA_MENSAJE);
        String mensaje = inicio < 0 ? contexto : contexto.substring(inicio);
        if (mensaje.toLowerCase(Locale.ROOT).contains("gasto")) {
            return respuesta("Listo, sumé los gastos.", List.of(
                    new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))));
        }
        if (!contexto.contains(PromptTaller.SIN_PIEZAS)) {
            return respuesta("Sigamos con tu sistema.", List.of(opciones(PREGUNTA_AJUSTE, OPCIONES_AJUSTE)));
        }
        return kitDe(contexto).map(this::conKit).orElseGet(this::preguntarRubro);
    }

    /** El id que sigue a la marca del kit en el contexto, si está y es un kit conocido. */
    private Optional<KitRubro> kitDe(String contexto) {
        int marca = contexto.indexOf(PromptTaller.MARCA_KIT);
        if (marca < 0) {
            return Optional.empty();
        }
        String id = contexto.substring(marca + PromptTaller.MARCA_KIT.length()).strip().split("\\s", 2)[0];
        return kitsRubro.buscar(id);
    }

    private RespuestaConHerramientas conKit(KitRubro kit) {
        LlamadaHerramienta pregunta = opciones(kit.pregunta().pregunta(), kit.pregunta().opciones());
        String piezas = kit.piezas().stream().map(PiezaKit::nombre).collect(Collectors.joining(", "));
        return respuesta("Te armé una base para %s: %s. Mirá cómo queda."
                        .formatted(kit.nombre().toLowerCase(Locale.ROOT), piezas),
                List.of(new LlamadaHerramienta("usar_kit", Map.of("rubro", kit.id())), pregunta));
    }

    private RespuestaConHerramientas preguntarRubro() {
        List<String> rubros = kitsRubro.kits().stream().limit(RUBROS_OFRECIDOS).map(KitRubro::nombre).toList();
        return respuesta("¡Hola! Te armo el sistema de tu negocio con piezas listas para usar.",
                List.of(opciones(PREGUNTA_RUBRO, rubros)));
    }

    private LlamadaHerramienta opciones(String pregunta, List<String> opciones) {
        return new LlamadaHerramienta("ofrecer_opciones", Map.of("pregunta", pregunta, "opciones", opciones));
    }

    private RespuestaConHerramientas respuesta(String texto, List<LlamadaHerramienta> llamadas) {
        return new RespuestaConHerramientas(texto, llamadas, PROVEEDOR, PROVEEDOR, 0, 0);
    }
}
