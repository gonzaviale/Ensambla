package com.ensambla.backend.domain.model.lote;

/** Los bytes de una foto del cuaderno ya leídos del almacenamiento, con su tipo para poder mostrarla. */
public record ArchivoFoto(byte[] contenido, String tipoContenido) {
}
