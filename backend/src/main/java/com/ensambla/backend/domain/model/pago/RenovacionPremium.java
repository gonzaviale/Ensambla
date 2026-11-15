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
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenovacionPremium {

    private static final String ESTADO_PREPARANDO = "PREPARANDO";

    private static final String ESTADO_CANCELADA = "CANCELADA";

    private static final String UNIDAD_DIAS = "days";

    private static final int FRECUENCIA_EN_DIAS = 30;

    private static final Map<String, String> ESTADOS_LOCALES = Map.of(
            "pending", "PENDIENTE",
            "authorized", "AUTORIZADA",
            "paused", "PAUSADA",
            "cancelled", ESTADO_CANCELADA,
            "canceled", ESTADO_CANCELADA);

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

    public static RenovacionPremium nueva(NegocioId negocioId, BigDecimal importe, String moneda,
            OffsetDateTime primerCobroEn, OffsetDateTime ahora) {
        return RenovacionPremium.builder()
                .id(RenovacionId.nuevo())
                .negocioId(negocioId)
                .estado(ESTADO_PREPARANDO)
                .importe(importe)
                .moneda(moneda)
                .primerCobroEn(primerCobroEn)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** El estado propio que corresponde al que informa el proveedor. */
    public static String estadoLocal(String estadoDelProveedor) {
        return Optional.ofNullable(ESTADOS_LOCALES.get(estadoDelProveedor)).orElseThrow(PasarelaPagoException::new);
    }

    public boolean estaCancelada() {
        return ESTADO_CANCELADA.equals(estado);
    }

    public boolean tieneContratoEnElProveedor() {
        return proveedorId != null;
    }

    public boolean debeCancelarseEnElProveedor() {
        return cancelacionSolicitada && !estaCancelada();
    }

    /** Toma los datos del contrato que informa el proveedor, salvo que ya tengamos una versión más nueva. */
    public void actualizarDesde(ContratoRemoto remoto, OffsetDateTime ahora) {
        validarContrato(remoto);
        if (aceptaCambiosDe(remoto)) {
            copiarDatosDe(remoto);
        }
        this.sincronizadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    /** El contrato que informa el proveedor tiene que ser el que se creó para esta renovación. */
    public void validarContrato(ContratoRemoto remoto) {
        if (!esElContratoCreadoPara(remoto)) {
            throw new PasarelaPagoException();
        }
    }

    /** El pago que informa el proveedor tiene que ser de este contrato y no anterior a su primer cobro. */
    public void validarPago(PagoRemoto pago) {
        if (!esUnPagoDeEsteContrato(pago)) {
            throw new PasarelaPagoException();
        }
    }

    private boolean aceptaCambiosDe(ContratoRemoto remoto) {
        return modificadoProveedorEn == null || !remoto.modificadoEn().isBefore(modificadoProveedorEn);
    }

    private void copiarDatosDe(ContratoRemoto remoto) {
        this.proveedorId = remoto.id();
        this.enlace = remoto.enlace();
        adoptarEstadoDe(remoto);
        this.proximoCobroEn = remoto.proximoCobroEn();
        this.modificadoProveedorEn = remoto.modificadoEn();
    }

    /** CANCELADA es terminal: una lectura concurrente antigua no reabre el contrato. */
    private void adoptarEstadoDe(ContratoRemoto remoto) {
        if (!estaCancelada()) {
            this.estado = estadoLocal(remoto.estado());
        }
    }

    private boolean esElContratoCreadoPara(ContratoRemoto remoto) {
        return id.equals(remoto.referencia())
                && remoto.importe().compareTo(importe) == 0
                && moneda.equals(remoto.moneda())
                && remoto.frecuencia() == FRECUENCIA_EN_DIAS
                && UNIDAD_DIAS.equals(remoto.unidad())
                && remoto.inicioEn().isEqual(primerCobroEn)
                && (proveedorId == null || proveedorId.equals(remoto.id()));
    }

    private boolean esUnPagoDeEsteContrato(PagoRemoto pago) {
        return id.equals(pago.referencia())
                && pago.contratoId().equals(proveedorId)
                && pago.importe().compareTo(importe) == 0
                && moneda.equals(pago.moneda())
                && !pago.debitoEn().isBefore(primerCobroEn);
    }
}
