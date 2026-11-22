package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/** El cobro de un importe mostrando un QR manual o una orden verificada por Mercado Pago. */
public record CobroQr(CobroQrId id, NegocioId negocioId, DestinoDeCobro destino, BigDecimal importe,
                      EstadoCobroQr estado, OffsetDateTime creadoEn, OffsetDateTime venceEn,
                      OffsetDateTime confirmadoEn, ConfirmacionDeCobro confirmacion,
                      OffsetDateTime actualizadoEn, String proveedorOrdenId, String proveedorPagoId,
                      String qrDinamico, String qrEstatico, String estadoDetalle) {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);
    public static final BigDecimal IMPORTE_MINIMO_MERCADO_PAGO = new BigDecimal("15.00");

    public static CobroQr nuevo(CobroQrId id, NegocioId negocioId, DestinoDeCobro destino,
                                BigDecimal importe, OffsetDateTime ahora) {
        exigirDestinoCargado(destino);
        exigirImporteValido(importe);
        return new CobroQr(id, negocioId, destino, importe, EstadoCobroQr.PENDIENTE,
                ahora, ahora.plus(VIGENCIA), null, null, ahora, null, null, null, null, null);
    }

    public static CobroQr nuevoMercadoPago(CobroQrId id, NegocioId negocioId, BigDecimal importe,
                                            OrdenCobroQr orden, OffsetDateTime ahora) {
        exigirImporteMercadoPago(importe);
        exigirOrdenCoincidente(id, importe, orden);
        CobroQr cobro = new CobroQr(id, negocioId, DestinoDeCobro.SIN_CARGAR, importe,
                EstadoCobroQr.PENDIENTE, ahora, ahora.plus(VIGENCIA), null, null, ahora,
                orden.id(), orden.pagoId(), orden.qrDinamico(), orden.qrEstatico(), orden.detalle());
        return cobro.sincronizar(orden, ahora);
    }

    public static void exigirImporteMercadoPago(BigDecimal importe) {
        exigirImporteValido(importe);
        if (importe.compareTo(IMPORTE_MINIMO_MERCADO_PAGO) < 0) {
            throw new BusinessRuleException("Mercado Pago permite cobrar con QR desde $15");
        }
    }

    public EstadoCobroQr estadoEn(OffsetDateTime ahora) {
        return estado.segunVencimiento(!ahora.isBefore(venceEn));
    }

    public CobroQr confirmarManualmente(OffsetDateTime ahora) {
        estadoEn(ahora).exigirQueAdmitaConfirmacion();
        return conEstado(EstadoCobroQr.COBRADO, ahora, ahora, ConfirmacionDeCobro.MANUAL);
    }

    public CobroQr cancelar(OffsetDateTime ahora) {
        estadoEn(ahora).exigirQueAdmitaCancelacion();
        return conEstado(EstadoCobroQr.CANCELADO, ahora, null, null);
    }

    public CobroQr sincronizar(OrdenCobroQr orden, OffsetDateTime ahora) {
        exigirOrdenCoincidente(id, importe, orden);
        EstadoCobroQr nuevoEstado = estado.sincronizadoCon(orden.estado().estadoDelCobro());
        OffsetDateTime acreditadoEn = nuevoEstado == EstadoCobroQr.COBRADO
                ? confirmadoEn == null ? ahora : confirmadoEn : confirmadoEn;
        ConfirmacionDeCobro nuevaConfirmacion = nuevoEstado == EstadoCobroQr.COBRADO
                ? ConfirmacionDeCobro.MERCADO_PAGO : confirmacion;
        return new CobroQr(id, negocioId, destino, importe, nuevoEstado, creadoEn, venceEn,
                acreditadoEn, nuevaConfirmacion, ahora, orden.id(), orden.pagoId(),
                conservar(orden.qrDinamico(), qrDinamico), conservar(orden.qrEstatico(), qrEstatico), orden.detalle());
    }

    public void exigirAcreditadoPorMercadoPago() {
        estado.exigirQueEsteCobrado();
    }

    public boolean usaMercadoPago() {
        return proveedorOrdenId != null;
    }

    public void exigirTransferenciaPor(BigDecimal transferido) {
        if (importe.compareTo(transferido) != 0) {
            throw new BusinessRuleException("El importe transferido no coincide con el del cobro con QR");
        }
    }

    private CobroQr conEstado(EstadoCobroQr nuevoEstado, OffsetDateTime ahora,
                              OffsetDateTime confirmadoEn, ConfirmacionDeCobro confirmacion) {
        return new CobroQr(id, negocioId, destino, importe, nuevoEstado, creadoEn, venceEn,
                confirmadoEn, confirmacion, ahora, proveedorOrdenId, proveedorPagoId,
                qrDinamico, qrEstatico, estadoDetalle);
    }

    private static String conservar(String nuevo, String anterior) {
        return nuevo == null || nuevo.isBlank() ? anterior : nuevo;
    }

    private static void exigirOrdenCoincidente(CobroQrId id, BigDecimal importe, OrdenCobroQr orden) {
        if (!id.equals(orden.referencia()) || importe.compareTo(orden.importe()) != 0) {
            throw new BusinessRuleException("La orden de Mercado Pago no coincide con el cobro con QR");
        }
    }

    private static void exigirDestinoCargado(DestinoDeCobro destino) {
        if (!destino.estaCargado()) {
            throw new BusinessRuleException("Cargá tu alias o CVU en Mi negocio para cobrar con QR");
        }
    }

    private static void exigirImporteValido(BigDecimal importe) {
        if (importe.signum() <= 0 || importe.scale() > 2) {
            throw new BusinessRuleException(
                    "El importe del cobro con QR debe ser mayor a cero con hasta dos decimales");
        }
    }
}
