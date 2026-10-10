package com.ensambla.backend.infrastructure.ia;

import org.springframework.core.io.Resource;

/**
 * Cliente sin IA real: siempre devuelve la misma lectura de ejemplo. Sirve para desarrollar el front, los tests E2E
 * (T-037) y la demo sin gastar en el proveedor. Se activa con {@code ensambla.ia.proveedor=simulado}.
 */
public class ClienteModeloVisionSimulado implements ClienteModeloVision {

    static final String PROVEEDOR = "simulado";
    static final String MODELO = "lectura-de-ejemplo";

    private final String lecturaDeEjemplo;

    public ClienteModeloVisionSimulado(Resource lecturaDeEjemplo) {
        this.lecturaDeEjemplo = RecursoTexto.contenido(lecturaDeEjemplo);
    }

    @Override
    public RespuestaModeloVision completar(PeticionModeloVision peticion) {
        return new RespuestaModeloVision(lecturaDeEjemplo, PROVEEDOR, MODELO, 0, 0);
    }
}
