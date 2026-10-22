package com.ensambla.backend.domain.model.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoPremium {

    private String facturaId;

    private UUID negocioId;

    private UUID renovacionId;

    private String pagoId;

    private String estado;

    private OffsetDateTime inicioEn;

    private OffsetDateTime finEn;

    private OffsetDateTime modificadoProveedorEn;

    private OffsetDateTime actualizadoEn;
}
