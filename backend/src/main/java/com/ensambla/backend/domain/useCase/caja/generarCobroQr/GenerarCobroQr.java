package com.ensambla.backend.domain.useCase.caja.generarCobroQr;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class GenerarCobroQr {

    private final NegocioRepository negocioRepository;
    private final CobroQrRepository cobroQrRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobroQrItem execute(GenerarCobroQrRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return CobroQrItem.de(generarEnTransaccion(request, ahora), ahora);
    }

    private CobroQr generarEnTransaccion(GenerarCobroQrRequest request, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> generar(request, ahora));
    }

    /** Repetir el mismo cobro devuelve el que ya estaba; con otro importe, se rechaza. */
    private CobroQr generar(GenerarCobroQrRequest request, OffsetDateTime ahora) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        return buscarCobro(request.id(), negocio)
                .map(anterior -> repetirCobro(anterior, request))
                .orElseGet(() -> generarCobroNuevo(negocio, request, ahora));
    }

    private CobroQr repetirCobro(CobroQr anterior, GenerarCobroQrRequest request) {
        if (anterior.importe().compareTo(request.importe()) != 0) {
            throw new DuplicateResourceException("Este cobro con QR ya se generó por otro importe");
        }
        return anterior;
    }

    private CobroQr generarCobroNuevo(Negocio negocio, GenerarCobroQrRequest request, OffsetDateTime ahora) {
        return guardar(CobroQr.nuevo(request.id(), negocio.getId(), negocio.destinoDeCobro(),
                request.importe(), ahora));
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Optional<CobroQr> buscarCobro(CobroQrId id, Negocio negocio) {
        return cobroQrRepository.findByIdAndNegocioId(id, negocio.getId());
    }

    private CobroQr guardar(CobroQr cobro) {
        return cobroQrRepository.save(cobro);
    }
}
