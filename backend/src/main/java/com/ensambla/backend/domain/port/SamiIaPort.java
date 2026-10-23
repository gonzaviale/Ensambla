package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;

/** Respaldo con IA de Sami (T-117): se consulta sólo cuando el diccionario y las preguntas no alcanzan. */
public interface SamiIaPort {

    /** Puede lanzar cualquier excepción no verificada; el servicio la traduce a un error de regla de negocio. */
    PropuestaIaSami proponer(ConsultaIaSami consulta);
}
