package com.ensambla.backend.domain.model.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
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

    /** El contrato que informa el proveedor tiene que ser el que se creó para esta renovación. */
    public void validarContrato(ContratoRemoto remoto) {
        if (!id.equals(remoto.referencia()) || remoto.importe().compareTo(importe) != 0
                || !moneda.equals(remoto.moneda()) || remoto.frecuencia() != 30
                || !"days".equals(remoto.unidad()) || !remoto.inicioEn().isEqual(primerCobroEn)
                || (proveedorId != null && !proveedorId.equals(remoto.id()))) {
            throw new PasarelaPagoException();
        }
    }

    /** El estado propio que corresponde al que informa el proveedor. */
    public static String estadoLocal(String estadoDelProveedor) {
        return switch (estadoDelProveedor) {
            case "pending" -> "PENDIENTE";
            case "authorized" -> "AUTORIZADA";
            case "paused" -> "PAUSADA";
            case "cancelled", "canceled" -> "CANCELADA";
            default -> throw new PasarelaPagoException();
        };
    }
}
