package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que se leyó de una foto suelta, con las claves de quien llama.
 *
 * @param filas        una por renglón; sus datos van por la clave del campo del bloque o la de una columna nueva
 * @param camposNuevos columnas de la foto que el bloque no tiene; vacía si el bloque no existe todavía
 * @param advertencias avisos de la lectura para mostrar a la persona
 */
public record LecturaImagen(List<Map<String, DatoLeido>> filas, List<CampoPropuesto> camposNuevos,
                            List<String> advertencias) {

    public LecturaImagen {
        filas = List.copyOf(filas);
        camposNuevos = List.copyOf(camposNuevos);
        advertencias = List.copyOf(advertencias);
    }

    /**
     * Traduce la propuesta a las claves del bloque. Las columnas nuevas reciben una clave que no pisa la de un campo
     * del bloque ni la de otra columna nueva, y sólo se conservan si el bloque ya existe: si no, la persona lo arma
     * como quiera.
     */
    public static LecturaImagen desde(PropuestaLectura propuesta, BloqueDeImagen bloque, boolean destinoExistente) {
        Map<UUID, String> clavesPorCampo = bloque.clavesPorCampo();
        Map<String, CampoPropuesto> nuevas = destinoExistente
                ? clavesLibres(propuesta.camposNuevos(), new HashSet<>(clavesPorCampo.values()))
                : Map.of();
        List<Map<String, DatoLeido>> filas = propuesta.lectura().filas().stream()
                .map(fila -> datosPorClave(fila, propuesta.mapeoCampos(), clavesPorCampo, nuevas))
                .toList();
        return new LecturaImagen(filas, List.copyOf(nuevas.values()), propuesta.lectura().advertencias());
    }

    /** Las columnas nuevas con su clave definitiva, por la clave con que las leyó la IA. */
    private static Map<String, CampoPropuesto> clavesLibres(List<CampoPropuesto> propuestas, Set<String> usadas) {
        Map<String, CampoPropuesto> nuevas = new LinkedHashMap<>();
        for (CampoPropuesto propuesta : propuestas) {
            String clave = claveLibre(propuesta.nombre(), usadas);
            usadas.add(clave);
            nuevas.put(propuesta.clave(), new CampoPropuesto(
                    clave, propuesta.nombre(), propuesta.tipo(), propuesta.opciones()));
        }
        return nuevas;
    }

    private static Map<String, DatoLeido> datosPorClave(FilaLeida fila, Map<String, UUID> mapeoCampos,
                                                         Map<UUID, String> clavesPorCampo,
                                                         Map<String, CampoPropuesto> nuevas) {
        Map<String, DatoLeido> datos = new LinkedHashMap<>();
        fila.datos().forEach((claveLeida, dato) -> {
            String clave = claveDefinitiva(claveLeida, mapeoCampos, clavesPorCampo, nuevas);
            if (clave != null) {
                datos.put(clave, dato);
            }
        });
        return datos;
    }

    /** {@code null} si la columna no es del bloque ni se propuso como nueva: ese dato se descarta. */
    private static String claveDefinitiva(String claveLeida, Map<String, UUID> mapeoCampos,
                                          Map<UUID, String> clavesPorCampo, Map<String, CampoPropuesto> nuevas) {
        UUID campoId = mapeoCampos.get(claveLeida);
        if (campoId != null) {
            return clavesPorCampo.get(campoId);
        }
        CampoPropuesto nueva = nuevas.get(claveLeida);
        return nueva == null ? null : nueva.clave();
    }

    private static String claveLibre(String nombre, Set<String> usadas) {
        String base = TextoNormalizado.de(nombre).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "columna";
        }
        String clave = base;
        for (int numero = 2; usadas.contains(clave); numero++) {
            clave = base + "_" + numero;
        }
        return clave;
    }
}
