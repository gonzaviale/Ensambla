package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Una página del cuaderno subida en un lote. La imagen vive en el almacenamiento privado; acá sólo la clave. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoCuaderno {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private FotoId id;

    private NegocioId negocioId;

    private LoteId loteId;

    private String claveAlmacenamiento;

    private String tipoContenido;

    private Long tamanioBytes;

    private Short rotacion;

    private Short posicion;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** Bytes del archivo mientras se sube; no se persiste. */
    private byte[] contenido;

    /**
     * Valida el archivo y arma la foto. La clave empieza con el id del negocio, que es la carpeta privada que lee el
     * procesamiento de IA ({@code ArchivoCuadernoPort}).
     *
     * @throws BusinessRuleException si el archivo está vacío, supera {@link #MAX_BYTES} o no es un formato aceptado
     */
    public static FotoCuaderno nueva(NegocioId negocioId, byte[] contenido, short posicion, OffsetDateTime ahora) {
        TipoFoto tipo = validar(contenido, posicion);
        return FotoCuaderno.builder()
                .negocioId(negocioId)
                .claveAlmacenamiento(clave(negocioId, tipo))
                .tipoContenido(tipo.getMediaType())
                .tamanioBytes((long) contenido.length)
                .rotacion((short) 0)
                .posicion(posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .contenido(contenido)
                .build();
    }

    private static String clave(NegocioId negocioId, TipoFoto tipo) {
        return negocioId + "/fotos/" + FotoId.nuevo() + "." + tipo.getExtension();
    }

    private static TipoFoto validar(byte[] contenido, short posicion) {
        String foto = "La foto " + (posicion + 1);
        if (contenido.length == 0) {
            throw new BusinessRuleException(foto + " está vacía");
        }
        if (contenido.length > MAX_BYTES) {
            throw new BusinessRuleException(foto + " supera el máximo de 10 MB");
        }
        return TipoFoto.detectar(contenido).orElseThrow(() -> new BusinessRuleException(
                foto + " no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF"));
    }
}
