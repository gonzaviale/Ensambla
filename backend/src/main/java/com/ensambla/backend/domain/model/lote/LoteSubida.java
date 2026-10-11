package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Una tanda de fotos del cuaderno que se sube junta para que la IA la lea. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteSubida {

    /** Coincide con lo que la IA lee de una vez ({@code ensambla.ia.max-paginas-por-lectura}). */
    public static final int MAX_FOTOS = 10;

    private UUID id;

    private UUID negocioId;

    private EstadoLote estado;

    private Short intentos;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    @Builder.Default
    private List<FotoCuaderno> fotos = List.of();

    /**
     * Arma un lote pendiente con las fotos en el orden recibido.
     *
     * @throws BusinessRuleException si no hay fotos, hay más de {@link #MAX_FOTOS} o alguna no es válida
     */
    public static LoteSubida nuevo(UUID negocioId, List<byte[]> contenidos, OffsetDateTime ahora) {
        validarCantidad(contenidos);
        List<FotoCuaderno> fotos = new ArrayList<>();
        for (byte[] contenido : contenidos) {
            fotos.add(FotoCuaderno.nueva(negocioId, contenido, (short) fotos.size(), ahora));
        }
        return LoteSubida.builder()
                .negocioId(negocioId)
                .estado(EstadoLote.PENDIENTE)
                .intentos((short) 0)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .fotos(fotos)
                .build();
    }

    private static void validarCantidad(List<byte[]> contenidos) {
        if (contenidos.isEmpty()) {
            throw new BusinessRuleException("Subí al menos una foto");
        }
        if (contenidos.size() > MAX_FOTOS) {
            throw new BusinessRuleException("Podés subir hasta " + MAX_FOTOS + " fotos por lote");
        }
    }
}
