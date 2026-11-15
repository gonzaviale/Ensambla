package com.ensambla.backend.domain.model.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
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

    private static final long HORAS_DE_PREMIUM_POR_PAGO = 720;

    private String facturaId;

    private NegocioId negocioId;

    private RenovacionId renovacionId;

    private String pagoId;

    private String estado;

    private OffsetDateTime inicioEn;

    private OffsetDateTime finEn;

    private OffsetDateTime modificadoProveedorEn;

    private OffsetDateTime actualizadoEn;

    public static PagoPremium de(PagoRemoto remoto, RenovacionPremium renovacion, OffsetDateTime ahora) {
        return PagoPremium.builder()
                .facturaId(remoto.facturaId())
                .negocioId(renovacion.getNegocioId())
                .renovacionId(renovacion.getId())
                .pagoId(remoto.pagoId())
                .estado(remoto.estado())
                .inicioEn(remoto.debitoEn())
                .finEn(remoto.debitoEn().plusHours(HORAS_DE_PREMIUM_POR_PAGO))
                .modificadoProveedorEn(remoto.modificadoEn())
                .actualizadoEn(ahora)
                .build();
    }

    /** El proveedor no puede informar con la misma factura un pago de otra renovación o de otro día. */
    public void validarMismoPago(RenovacionId otraRenovacionId, PagoRemoto remoto) {
        if (!renovacionId.equals(otraRenovacionId) || !inicioEn.isEqual(remoto.debitoEn())) {
            throw new PasarelaPagoException();
        }
    }

    public boolean estaAlDiaCon(PagoRemoto remoto) {
        return !remoto.modificadoEn().isAfter(modificadoProveedorEn);
    }
}
