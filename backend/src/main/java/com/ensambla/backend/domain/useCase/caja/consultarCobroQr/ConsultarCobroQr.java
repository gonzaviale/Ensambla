package com.ensambla.backend.domain.useCase.caja.consultarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ConsultarCobroQr {

    private final NegocioRepository negocioRepository;
    private final CobroQrRepository cobroQrRepository;
    private final PasarelaCobrosQrPort pasarelaCobrosQrPort;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobroQrItem execute(ConsultarCobroQrRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request);
        CobroQr cobro = obtenerCobro(request, negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CobroQrItem.de(sincronizarSiCorresponde(cobro, ahora), ahora);
    }

    private NegocioId obtenerNegocioDelUsuario(ConsultarCobroQrRequest request) {
        return negocioRepository.obtenerIdDelUsuario(request.usuarioId());
    }

    private CobroQr obtenerCobro(ConsultarCobroQrRequest request, NegocioId negocioId) {
        return cobroQrRepository.obtener(request.id(), negocioId);
    }

    private CobroQr sincronizarSiCorresponde(CobroQr cobro, OffsetDateTime ahora) {
        if (!cobro.usaMercadoPago() || cobro.estado().estaFinalizado()) {
            return cobro;
        }
        CobroQr actualizado = cobro.sincronizar(consultarOrden(cobro), ahora);
        return guardarEnTransaccion(actualizado);
    }

    private com.ensambla.backend.domain.model.caja.OrdenCobroQr consultarOrden(CobroQr cobro) {
        return pasarelaCobrosQrPort.consultar(cobro.proveedorOrdenId());
    }

    private CobroQr guardarEnTransaccion(CobroQr actualizado) {
        return transaccionPort.ejecutar(() -> cobroQrRepository.save(actualizado));
    }
}
