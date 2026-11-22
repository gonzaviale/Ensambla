package com.ensambla.backend.domain.useCase.caja.recibirNotificacionCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RecibirNotificacionCobroQr {

    private final PasarelaCobrosQrPort pasarelaCobrosQrPort;
    private final CobroQrRepository cobroQrRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public void execute(String tipo, String recursoId, String firma, String solicitudId) {
        validarNotificacion(firma, solicitudId, recursoId);
        if (!"order".equals(tipo)) {
            return;
        }
        OrdenCobroQr orden = consultarOrden(recursoId);
        sincronizarCobroEncontrado(orden);
    }

    private void validarNotificacion(String firma, String solicitudId, String recursoId) {
        pasarelaCobrosQrPort.validarNotificacion(firma, solicitudId, recursoId);
    }

    private OrdenCobroQr consultarOrden(String recursoId) {
        return pasarelaCobrosQrPort.consultar(recursoId);
    }

    private void sincronizarCobroEncontrado(OrdenCobroQr orden) {
        cobroQrRepository.findByProveedorOrdenId(orden.id()).ifPresent(cobro -> sincronizar(cobro, orden));
    }

    private void sincronizar(CobroQr cobro, OrdenCobroQr orden) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        transaccionPort.ejecutar(() -> cobroQrRepository.save(cobro.sincronizar(orden, ahora)));
    }
}
