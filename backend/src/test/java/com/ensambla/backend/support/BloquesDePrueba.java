package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;

/** Bloques y conexiones armados en memoria para probar los casos de uso de tablas. */
public final class BloquesDePrueba {

    public static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    public static final BloqueId OTRO_BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    private BloquesDePrueba() {
    }

    public static Bloque unBloqueDelNegocio(BloqueId bloqueId, String nombre) {
        return Bloque.builder().id(bloqueId).negocioId(NEGOCIO_ID).nombre(nombre).build();
    }

    public static Campo unaConexionDesde(BloqueId bloqueId, String nombre) {
        return Campo.builder().bloqueId(bloqueId).negocioId(NEGOCIO_ID).nombre(nombre).build();
    }
}
