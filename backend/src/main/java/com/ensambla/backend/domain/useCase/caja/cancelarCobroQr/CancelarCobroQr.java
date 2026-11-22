package com.ensambla.backend.domain.useCase.caja.cancelarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CancelarCobroQr {

    private final NegocioRepository negocioRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final CobroQrRepository cobroQrRepository;
    private final PasarelaCobrosQrPort pasarelaCobrosQrPort;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobroQrItem execute(CancelarCobroQrRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return CobroQrItem.de(cancelarEnTransaccion(negocioId, request.id(), ahora), ahora);
    }

    private CobroQr cancelarEnTransaccion(NegocioId negocioId, CobroQrId id, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> cancelar(negocioId, id, ahora));
    }

    /** Bajo el mismo bloqueo del negocio que usa el cobro de la venta, para que no se crucen. */
    private CobroQr cancelar(NegocioId negocioId, CobroQrId id, OffsetDateTime ahora) {
        bloquearNegocio(negocioId);
        CobroQr cobro = obtenerCobro(id, negocioId);
        if (!cobro.usaMercadoPago()) {
            return guardar(cobro.cancelar(ahora));
        }
        CobroQr conciliado = cobro.sincronizar(
                pasarelaCobrosQrPort.consultar(cobro.proveedorOrdenId()), ahora);
        if (conciliado.estado().estaFinalizado()) {
            return guardar(conciliado);
        }
        return guardar(conciliado.sincronizar(
                pasarelaCobrosQrPort.cancelar(cobro.proveedorOrdenId()), ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private CobroQr obtenerCobro(CobroQrId id, NegocioId negocioId) {
        return cobroQrRepository.obtener(id, negocioId);
    }

    private CobroQr guardar(CobroQr cobro) {
        return cobroQrRepository.save(cobro);
    }
}
