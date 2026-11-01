package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.campo.CampoId;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Totales de los registros que cumplen una consulta, sin importar la página: cuántos son y la suma de cada campo
 * numérico del bloque.
 */
public record TotalesRegistros(long cantidad, Map<CampoId, BigDecimal> sumas) {

    /** Una suma por cada campo numérico, en el orden dado; un campo que ningún registro cargó suma cero. */
    public static TotalesRegistros armar(long cantidad, List<CampoId> camposNumericos,
                                         Map<CampoId, BigDecimal> sumasPorCampo) {
        Map<CampoId, BigDecimal> sumas = new LinkedHashMap<>();
        for (CampoId campoId : camposNumericos) {
            sumas.put(campoId, sumasPorCampo.getOrDefault(campoId, BigDecimal.ZERO));
        }
        return new TotalesRegistros(cantidad, sumas);
    }
}
