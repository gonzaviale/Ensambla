package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;

import java.util.List;

/**
 * Lo que se le manda al modelo, independiente del proveedor.
 *
 * @param instrucciones prompt de sistema con las reglas de lectura
 * @param contexto      datos del negocio que ayudan a leer (bloques existentes)
 * @param paginas       imágenes en orden
 * @param esquemaJson   JSON Schema que debe cumplir la respuesta (salida estructurada)
 */
public record PeticionModeloVision(String instrucciones, String contexto, List<PaginaCuaderno> paginas,
                                   String esquemaJson) {
}
