package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;

import java.util.List;

public record BloqueLectura(BloqueId id, String nombre, List<CampoConocido> campos) {

    public BloqueLectura {
        campos = List.copyOf(campos);
    }

    /** El bloque tal como se le muestra a la IA; {@code bloques} sirve para nombrar las tablas que conecta. */
    public BloqueConocido paraElLector(List<BloqueLectura> bloques) {
        return new BloqueConocido(id, nombre, campos.stream()
                .map(campo -> new CampoContexto(campo.nombre(), campo.tipo(), campo.opciones(),
                        nombreConectado(campo, bloques)))
                .toList());
    }

    private static String nombreConectado(CampoConocido campo, List<BloqueLectura> bloques) {
        return bloques.stream()
                .filter(bloque -> bloque.id().equals(campo.bloqueRelacionadoId()))
                .map(BloqueLectura::nombre)
                .findFirst()
                .orElse(null);
    }
}
