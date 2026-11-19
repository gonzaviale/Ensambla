package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.registro.MovimientoDeStock;

import java.util.List;

/** Lo que se guardó al confirmar una carga, para contárselo a la persona. */
record ResultadoDelGuardado(String tabla, int nuevas, int actualizadas, List<String> sumados,
                            List<MovimientoDeStock> stock) {
}
