package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;

import java.util.List;

/** Cómo se vuelven a armar las filas de una propuesta cuando la persona eligió la tabla: depende de si vino de un texto o de una foto. */
interface ReinterpretarPropuesta {

    List<FilaPropuesta> filasPara(PropuestaWhatsapp propuesta, BloqueId destino, TablaDisponible tabla,
                                  List<TablaDisponible> tablas, Atencion atencion);
}
