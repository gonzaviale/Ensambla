package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Respuesta cruda de la IA: plantilla elegida y columnas extra. Nunca se confía en ella sin pasar por
 * {@link #aInterpretacion(List)}, que descarta lo que no existe o no sirve.
 */
public record PropuestaIaSami(
        String plantilla,
        List<ColumnaSugerida> columnasExtra,
        double confianza,
        ConsumoIaSami consumo
) {

    public static final int MAX_COLUMNAS = 6;
    public static final int MAX_LARGO_NOMBRE = 60;
    static final Set<TipoCampo> TIPOS_PERMITIDOS =
            EnumSet.of(TipoCampo.TEXTO, TipoCampo.NUMERO, TipoCampo.FECHA, TipoCampo.OPCION, TipoCampo.SI_NO);

    /**
     * @return la interpretación si la plantilla elegida existe entre las disponibles; vacío si la IA inventó una.
     *         Las columnas inválidas, repetidas o que la plantilla ya trae se descartan, y se conservan
     *         {@value #MAX_COLUMNAS} como máximo.
     */
    public Optional<InterpretacionSami> aInterpretacion(List<PlantillaProyecto> disponibles) {
        return disponibles.stream()
                .filter(candidata -> plantilla != null && candidata.nombre().equalsIgnoreCase(plantilla.trim()))
                .findFirst()
                .map(elegida -> new InterpretacionSami(
                        elegida.id(),
                        elegida.nombre(),
                        columnasValidas(elegida),
                        Math.max(0.0, Math.min(1.0, confianza)),
                        List.of()));
    }

    private List<ColumnaSugerida> columnasValidas(PlantillaProyecto elegida) {
        Set<String> usadas = new HashSet<>();
        elegida.estructura().bloques().forEach(bloque -> bloque.campos()
                .forEach(campo -> usadas.add(campo.nombre().toLowerCase())));
        List<ColumnaSugerida> validas = new ArrayList<>();
        for (ColumnaSugerida columna : columnasExtra) {
            if (esValida(columna) && usadas.add(columna.nombre().trim().toLowerCase())) {
                validas.add(new ColumnaSugerida(columna.nombre().trim(), columna.tipo(), opcionesDe(columna)));
            }
        }
        return validas.stream().limit(MAX_COLUMNAS).toList();
    }

    private boolean esValida(ColumnaSugerida columna) {
        return columna.nombre() != null
                && !columna.nombre().isBlank()
                && columna.nombre().trim().length() <= MAX_LARGO_NOMBRE
                && TIPOS_PERMITIDOS.contains(columna.tipo())
                && (columna.tipo() != TipoCampo.OPCION || opcionesDe(columna).size() >= 2);
    }

    private List<String> opcionesDe(ColumnaSugerida columna) {
        if (columna.tipo() != TipoCampo.OPCION) {
            return List.of();
        }
        return columna.opciones().stream()
                .filter(opcion -> opcion != null && !opcion.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
