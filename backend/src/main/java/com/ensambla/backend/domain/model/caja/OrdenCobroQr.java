package com.ensambla.backend.domain.model.caja;

import java.math.BigDecimal;

/** La parte confiable de una orden QR leída desde la pasarela. */
public record OrdenCobroQr(String id, CobroQrId referencia, BigDecimal importe, EstadoOrdenCobroQr estado,
                           String detalle, String pagoId, String qrDinamico, String qrEstatico) {
}
