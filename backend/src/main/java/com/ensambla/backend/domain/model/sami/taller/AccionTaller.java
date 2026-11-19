package com.ensambla.backend.domain.model.sami.taller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una acción ya validada que el taller tiene que aplicar. Los argumentos usan el vocabulario del catálogo;
 * {@code pieza}, {@code desde} y {@code hacia} son el id de una pieza existente o, si se sumó en este mismo turno, su
 * tipo (una tabla a medida, que se repite, por su nombre).
 */
public record AccionTaller(String herramienta, Map<String, Object> argumentos) {

    private static final String EJEMPLOS = "ejemplos";

    public static Armado de(HerramientaTaller herramienta) {
        return new Armado(herramienta);
    }

    public boolean es(HerramientaTaller otra) {
        return otra.nombre().equals(herramienta);
    }

    /** Las filas de ejemplo sólo sirven para imaginar un sistema nuevo: la acción sin ellas. */
    public AccionTaller sinEjemplos() {
        if (!argumentos.containsKey(EJEMPLOS)) {
            return this;
        }
        Map<String, Object> sinLasFilas = new HashMap<>(argumentos);
        sinLasFilas.remove(EJEMPLOS);
        return new AccionTaller(herramienta, sinLasFilas);
    }

    /** Arma los argumentos de una acción en el orden en que viajan. */
    public static final class Armado {

        private final HerramientaTaller herramienta;
        private final Map<String, Object> argumentos = new LinkedHashMap<>();

        private Armado(HerramientaTaller herramienta) {
            this.herramienta = herramienta;
        }

        public Armado con(String clave, Object valor) {
            argumentos.put(clave, valor);
            return this;
        }

        public Armado conTodos(Map<String, Object> otros) {
            argumentos.putAll(otros);
            return this;
        }

        public Armado conSiHay(String clave, Optional<?> valor) {
            valor.ifPresent(presente -> argumentos.put(clave, presente));
            return this;
        }

        public Armado conSiNoEstaVacia(String clave, List<?> lista) {
            if (!lista.isEmpty()) {
                argumentos.put(clave, lista);
            }
            return this;
        }

        public AccionTaller armar() {
            return new AccionTaller(herramienta.nombre(), argumentos);
        }
    }
}
