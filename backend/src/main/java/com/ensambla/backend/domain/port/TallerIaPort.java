package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;

/** IA del taller de Sami: conversa y pide herramientas. El dominio decide qué hacer con lo que pida. */
public interface TallerIaPort {

    /** Puede lanzar cualquier excepción no verificada; el servicio la traduce a un error de regla de negocio. */
    ResultadoTallerIa conversar(ConsultaTaller consulta);
}
