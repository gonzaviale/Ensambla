package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;

/**
 * IA con la que la persona le cuenta un objetivo a Sami: contesta lo que le preguntan, entiende el objetivo y elige
 * métricas del catálogo o redacta acciones. La IA no calcula nada: los números salen del servidor.
 */
public interface ConversacionObjetivosIaPort {

    /**
     * Puede lanzar cualquier excepción no verificada (también si la respuesta no sirve); quien llama arma entonces una
     * propuesta de respaldo.
     */
    PropuestaObjetivoIa conversar(ConsultaObjetivoIa consulta);
}
