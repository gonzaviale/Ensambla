package com.ensambla.backend.domain.model.lectura;

import java.util.Map;

/**
 * Un renglón del cuaderno convertido en registro a confirmar.
 *
 * @param datos valores indexados por la {@link CampoPropuesto#clave() clave} del campo
 */
public record FilaLeida(int pagina, int renglon, Map<String, DatoLeido> datos) {

    public FilaLeida {
        datos = Map.copyOf(datos);
    }

    public boolean tieneDudas(double umbral) {
        return datos.values().stream().anyMatch(dato -> dato.esDudoso(umbral));
    }
}
