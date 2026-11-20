package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.whatsapp.CoincidenciaDeFilas;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.VinculoPropuesto;

import java.util.List;
import java.util.Optional;

/** Las filas de una tabla que tienen nombre, cada una con el suyo en la misma posición. */
record FilasNombradas(List<Registro> registros, List<String> nombres) {

    /** La posición de la fila que la persona quiso decir con ese nombre, aunque no lo haya escrito tal cual. */
    Optional<Integer> buscar(String nombre) {
        return CoincidenciaDeFilas.buscar(nombre, nombres);
    }

    FilaPropuesta actualizando(FilaPropuesta fila, int posicion, String claveDelNombre) {
        Registro registro = registros.get(posicion);
        return fila.actualizando(registro.getId().toString(), nombres.get(posicion), claveDelNombre,
                registro.getValores());
    }

    VinculoPropuesto vinculoEn(int posicion, TablaDisponible tabla) {
        return new VinculoPropuesto(nombres.get(posicion), registros.get(posicion).getId().toString(),
                tabla.bloque().id().toString(), tabla.bloque().nombre());
    }
}
