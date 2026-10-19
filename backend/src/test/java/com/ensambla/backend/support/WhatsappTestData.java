package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntranteWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class WhatsappTestData {

    public static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    public static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID OTRO_NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000002");
    public static final UUID VINCULO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    public static final UUID MENSAJE_ID = UUID.fromString("e1000000-0000-4000-8000-000000000001");
    public static final String TELEFONO = "5491122334455";
    public static final String CODIGO = "482913";
    public static final String WA_MENSAJE_ID = "wamid.HBgNNTQ5MTEyMjMzNDQ1NRUCABIYFjNFQjBDMzQ1";
    public static final String MEDIA_ID = "1234567890123456";
    public static final String TEXTO = "Vendí 3 velas a Marta a 4500";
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    public static final OffsetDateTime RECIBIDO_EN = OffsetDateTime.parse("2026-10-19T11:59:30Z");

    private WhatsappTestData() {
    }

    /** Vínculo con el código vigente hasta 10 minutos después de {@link #AHORA}, todavía sin número. */
    public static VinculoWhatsapp vinculoConCodigo(UUID negocioId) {
        VinculoWhatsapp vinculo = VinculoWhatsapp.nuevo(negocioId, AHORA);
        vinculo.setId(VINCULO_ID);
        vinculo.asignarCodigo(CODIGO, AHORA);
        return vinculo;
    }

    public static VinculoWhatsapp vinculado(UUID negocioId, String telefono) {
        VinculoWhatsapp vinculo = VinculoWhatsapp.nuevo(negocioId, AHORA);
        vinculo.setId(VINCULO_ID);
        vinculo.vincular(telefono, AHORA);
        return vinculo;
    }

    public static MensajeEntranteWhatsapp texto(String texto) {
        return new MensajeEntranteWhatsapp(
                WA_MENSAJE_ID, TELEFONO, TipoMensajeWhatsapp.TEXTO, texto, null, RECIBIDO_EN);
    }

    public static MensajeEntranteWhatsapp audio() {
        return new MensajeEntranteWhatsapp(
                WA_MENSAJE_ID, TELEFONO, TipoMensajeWhatsapp.AUDIO, null, MEDIA_ID, RECIBIDO_EN);
    }
}
