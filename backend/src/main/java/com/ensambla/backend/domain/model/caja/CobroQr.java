package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * El cobro de un importe mostrando un QR. Ensambla no verifica el pago: el comerciante lo confirma a mano.
 * El estado guardado es PENDIENTE, COBRADO o CANCELADO; el vencimiento se deduce de {@code venceEn}.
 */
public record CobroQr(CobroQrId id, NegocioId negocioId, DestinoDeCobro destino, BigDecimal importe,
                      EstadoCobroQr estado, OffsetDateTime creadoEn, OffsetDateTime venceEn,
                      OffsetDateTime confirmadoEn, ConfirmacionDeCobro confirmacion,
                      OffsetDateTime actualizadoEn) {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);

    public static CobroQr nuevo(CobroQrId id, NegocioId negocioId, DestinoDeCobro destino,
                                BigDecimal importe, OffsetDateTime ahora) {
        exigirDestinoCargado(destino);
        exigirImporteValido(importe);
        return new CobroQr(id, negocioId, destino, importe, EstadoCobroQr.PENDIENTE,
                ahora, ahora.plus(VIGENCIA), null, null, ahora);
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

    public void exigirTransferenciaPor(BigDecimal transferido) {
        if (importe.compareTo(transferido) != 0) {
            throw new BusinessRuleException("El importe transferido no coincide con el del cobro con QR");
        }
    }

    private CobroQr conEstado(EstadoCobroQr nuevoEstado, OffsetDateTime ahora,
                              OffsetDateTime confirmadoEn, ConfirmacionDeCobro confirmacion) {
        return new CobroQr(id, negocioId, destino, importe, nuevoEstado, creadoEn, venceEn,
                confirmadoEn, confirmacion, ahora);
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
