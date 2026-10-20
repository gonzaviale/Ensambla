package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;

import java.util.List;

/** Lo que contestó el modelo: texto, herramientas que pidió (sin validar) y los tokens que informó el proveedor. */
public record RespuestaConHerramientas(
        String texto,
        List<LlamadaHerramienta> llamadas,
        String proveedor,
        String modelo,
        long tokensEntrada,
        long tokensSalida
) {
}
