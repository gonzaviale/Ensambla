package com.ensambla.backend.domain.model.pago;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoPremium {

    private String facturaId;

    private NegocioId negocioId;

    private RenovacionId renovacionId;

    private String pagoId;

    private String estado;

    private OffsetDateTime inicioEn;

    private OffsetDateTime finEn;

    private OffsetDateTime modificadoProveedorEn;

    private OffsetDateTime actualizadoEn;
}
