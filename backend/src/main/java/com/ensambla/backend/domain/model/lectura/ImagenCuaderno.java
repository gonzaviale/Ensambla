package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.TipoFoto;

import java.util.EnumSet;
import java.util.Set;

/**
 * Una foto suelta lista para leer con la IA, sin guardarla. El tipo se detecta por los primeros bytes del archivo y no
 * por lo que declara el navegador.
 *
 * @param tipoContenido media type detectado, por ejemplo {@code image/jpeg}
 */
public record ImagenCuaderno(byte[] contenido, String tipoContenido) {

    /** Los formatos que el lector de IA sabe ver: la subida de lotes acepta además HEIC y PDF. */
    private static final Set<TipoFoto> LEGIBLES = EnumSet.of(TipoFoto.JPEG, TipoFoto.PNG, TipoFoto.WEBP);

    /**
     * @throws BusinessRuleException si el archivo está vacío, supera {@link FotoCuaderno#MAX_BYTES} o no es JPG, PNG o WEBP
     */
    public static ImagenCuaderno de(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new BusinessRuleException("La imagen está vacía");
        }
        if (contenido.length > FotoCuaderno.MAX_BYTES) {
            throw new BusinessRuleException("La imagen pesa más de 10 MB. Elegí una más liviana");
        }
        TipoFoto tipo = TipoFoto.detectar(contenido)
                .filter(LEGIBLES::contains)
                .orElseThrow(() -> new BusinessRuleException("Elegí una imagen JPG, PNG o WEBP"));
        return new ImagenCuaderno(contenido, tipo.getMediaType());
    }

    public PaginaCuaderno comoPagina() {
        return new PaginaCuaderno(1, contenido, tipoContenido);
    }
}
