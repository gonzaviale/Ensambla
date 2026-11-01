package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;

/** Pasa un texto escrito a mano («gasté 5000 en harina») a filas de las tablas del negocio. */
public interface InterpreteRegistrosIaPort {

    /** Un texto ilegible o sin tabla posible vuelve como interpretación vacía, con su consumo para registrarlo. */
    InterpretacionRegistros interpretar(ConsultaInterpretacion consulta);
}
