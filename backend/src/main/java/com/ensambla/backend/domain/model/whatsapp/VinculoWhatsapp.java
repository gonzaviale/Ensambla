package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Número de WhatsApp de un usuario, vinculado a su negocio: sólo desde él se puede cargar datos por chat. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VinculoWhatsapp {

    private VinculoWhatsappId id;

    private UsuarioId usuarioId;

    private NegocioId negocioId;

    private String telefono;

    private OffsetDateTime vinculadoEn;

    public static VinculoWhatsapp nuevo(UsuarioId usuarioId, NegocioId negocioId, TelefonoWhatsapp telefono,
                                        OffsetDateTime ahora) {
        return VinculoWhatsapp.builder()
                .usuarioId(usuarioId)
                .negocioId(negocioId)
                .telefono(telefono.valor())
                .vinculadoEn(ahora)
                .build();
    }
}
