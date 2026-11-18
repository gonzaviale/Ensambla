package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.List;

/**
 * Columna que la IA propone para el bloque.
 *
 * @param clave identificador estable dentro de la lectura; las filas indexan sus datos por esta clave
 */
public record CampoPropuesto(String clave, String nombre, TipoCampo tipo, List<String> opciones) {

    public CampoPropuesto {
        opciones = List.copyOf(opciones);
    }

    /** El campo del dominio que esta columna sería; sirve para preguntarle por sus valores. */
    public Campo comoCampo() {
        return Campo.builder().nombre(nombre).tipo(tipo).opciones(opciones).build();
    }
}
