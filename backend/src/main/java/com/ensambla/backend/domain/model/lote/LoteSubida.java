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
import java.util.Optional;
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

    /** Bloque al que la persona dijo que van las filas; {@code null} si la IA propone uno. */
    private UUID bloqueDestinoId;

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
    public static LoteSubida nuevo(UUID negocioId, List<byte[]> contenidos, UUID bloqueDestinoId,
                                  OffsetDateTime ahora) {
        validarCantidad(contenidos);
        List<FotoCuaderno> fotos = new ArrayList<>();
        for (byte[] contenido : contenidos) {
            fotos.add(FotoCuaderno.nueva(negocioId, contenido, (short) fotos.size(), ahora));
        }
        return LoteSubida.builder()
                .negocioId(negocioId)
                .bloqueDestinoId(bloqueDestinoId)
                .estado(EstadoLote.PENDIENTE)
                .intentos((short) 0)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .fotos(fotos)
                .build();
    }

    /**
     * Sólo una lectura terminada se puede confirmar, y una sola vez.
     *
     * @throws BusinessRuleException si el lote todavía se está leyendo, falló o ya se guardó
     */
    public void validarConfirmable() {
        if (estado == EstadoLote.CONFIRMADO) {
            throw new BusinessRuleException("Este cuaderno ya se guardó");
        }
        if (estado != EstadoLote.LISTO) {
            throw new BusinessRuleException("Este cuaderno todavía no terminó de leerse");
        }
    }

    /** La foto de la página indicada (1 = la primera subida), si el lote la tiene. */
    public Optional<FotoCuaderno> fotoDePagina(int pagina) {
        return fotos.stream()
                .filter(foto -> foto.getPosicion() == pagina - 1)
                .findFirst();
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
