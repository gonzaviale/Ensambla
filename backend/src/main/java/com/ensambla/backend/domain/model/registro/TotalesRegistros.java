package com.ensambla.backend.domain.model.registro;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Totales de los registros que cumplen una consulta, sin importar la página: cuántos son y la suma de cada campo
 * numérico del bloque.
 */
public record TotalesRegistros(long cantidad, Map<UUID, BigDecimal> sumas) {

    /** Una suma por cada campo numérico, en el orden dado; un campo que ningún registro cargó suma cero. */
    public static TotalesRegistros armar(long cantidad, List<UUID> camposNumericos,
                                         Map<UUID, BigDecimal> sumasPorCampo) {
        Map<UUID, BigDecimal> sumas = new LinkedHashMap<>();
        for (UUID campoId : camposNumericos) {
            sumas.put(campoId, sumasPorCampo.getOrDefault(campoId, BigDecimal.ZERO));
        }
        return new TotalesRegistros(cantidad, sumas);
    }
}
