package com.ensambla.backend.domain.model.pago;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenovacionPremium {

    private RenovacionId id;

    private NegocioId negocioId;

    private String proveedorId;

    private String enlace;

    private String estado;

    private BigDecimal importe;

    private String moneda;

    private OffsetDateTime primerCobroEn;

    private boolean cancelacionSolicitada;

    private OffsetDateTime proximoCobroEn;

    private OffsetDateTime modificadoProveedorEn;

    private OffsetDateTime sincronizadoEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
