package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ObjetivoTestData {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    public static final NegocioId NEGOCIO_ID = NegocioId.de("00000000-0000-4000-8000-000000000002");
    public static final ObjetivoId OBJETIVO_ID = ObjetivoId.de("00000000-0000-4000-8000-000000000005");
    public static final String TEXTO = "Quiero ganar $400.000 por mes";
    public static final BigDecimal META = new BigDecimal("400000.00");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-10T12:00:00Z");

    private ObjetivoTestData() {
    }

    public static Objetivo objetivo() {
        return Objetivo.builder()
                .id(OBJETIVO_ID)
                .negocioId(NEGOCIO_ID)
                .texto(TEXTO)
                .meta(META)
                .creadoEn(AHORA)
                .build();
    }
}
