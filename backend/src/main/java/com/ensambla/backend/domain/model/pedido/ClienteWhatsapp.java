package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Cliente que le pide a un comercio por WhatsApp. No tiene cuenta: lo identifica su teléfono. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClienteWhatsapp {

    private ClienteId id;

    /** Sólo dígitos, con código de país. */
    private String telefono;

    /** El último comercio al que le pidió; vacío si todavía no pidió. */
    private NegocioId ultimoNegocioId;

    private OffsetDateTime creadoEn;

    public static ClienteWhatsapp nuevo(String telefono, OffsetDateTime ahora) {
        return ClienteWhatsapp.builder().telefono(telefono).creadoEn(ahora).build();
    }
}
