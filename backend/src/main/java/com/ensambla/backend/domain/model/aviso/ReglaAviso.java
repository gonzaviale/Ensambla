package com.ensambla.backend.domain.model.aviso;

import java.util.List;

/**
 * Regla que el motor de monitoreo ejecuta sobre cada negocio. Para sumar una regla alcanza con registrarla como bean:
 * el motor recibe todas las que haya.
 */
public interface ReglaAviso {

    TipoAviso tipo();

    /** Un resultado por cada entidad del negocio que hoy merece el aviso; vacío si ninguna. */
    List<ResultadoRegla> evaluar(ContextoNegocio contexto);
}
