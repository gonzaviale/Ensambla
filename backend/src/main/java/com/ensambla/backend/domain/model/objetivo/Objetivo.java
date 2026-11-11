package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lo que la persona quiere lograr con su negocio y el resultado mensual que busca. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Objetivo {

    private ObjetivoId id;

    private NegocioId negocioId;

    /** Lo que la persona le contó a Sami, con sus palabras. */
    private String texto;

    /** Resultado mensual que se quiere alcanzar. */
    private BigDecimal meta;

    private OffsetDateTime creadoEn;

    public static Objetivo nuevo(NegocioId negocioId, String texto, BigDecimal meta, OffsetDateTime ahora) {
        return Objetivo.builder()
                .negocioId(negocioId)
                .texto(texto.strip())
                .meta(meta)
                .creadoEn(ahora)
                .build();
    }
}
