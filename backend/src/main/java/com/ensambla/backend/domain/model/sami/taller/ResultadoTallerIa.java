package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.util.List;

/** Respuesta cruda de la IA en un turno: texto, herramientas pedidas (sin validar) y lo que costó. */
public record ResultadoTallerIa(String texto, List<LlamadaHerramienta> llamadas, ConsumoIaSami consumo) {
}
