package com.ensambla.backend.domain.model.archivo;

import java.time.Instant;

/**
 * URL temporal para subir o leer un archivo del almacenamiento privado. Sin ella el archivo no es accesible.
 *
 * @param url      dirección firmada, lista para usar desde el navegador
 * @param expiraEn instante a partir del cual la firma deja de valer
 */
public record UrlFirmada(String url, Instant expiraEn) {
}
