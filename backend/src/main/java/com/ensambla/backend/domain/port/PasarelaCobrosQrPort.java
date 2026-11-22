package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;

import java.math.BigDecimal;

public interface PasarelaCobrosQrPort {

    boolean disponible();

    OrdenCobroQr crear(CobroQrId referencia, BigDecimal importe);

    OrdenCobroQr consultar(String ordenId);

    OrdenCobroQr cancelar(String ordenId);

    void validarNotificacion(String firma, String solicitudId, String recursoId);
}
