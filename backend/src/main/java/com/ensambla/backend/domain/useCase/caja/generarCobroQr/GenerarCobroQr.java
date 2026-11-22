package com.ensambla.backend.domain.useCase.caja.generarCobroQr;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
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
    private final PasarelaCobrosQrPort pasarelaCobrosQrPort;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobroQrItem execute(GenerarCobroQrRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Optional<CobroQr> anterior = buscarCobro(request.id(), negocio);
        if (anterior.isPresent()) {
            return CobroQrItem.de(repetirCobro(anterior.get(), request), ahora);
        }
        if (!pasarelaCobrosQrPort.disponible()) {
            return CobroQrItem.de(guardarManualEnTransaccion(negocio, request, ahora), ahora);
        }
        CobroQr.exigirImporteMercadoPago(request.importe());
        OrdenCobroQr orden = crearOrden(request);
        return CobroQrItem.de(guardarNuevoEnTransaccion(negocio, request, orden, ahora), ahora);
    }

    private CobroQr guardarManualEnTransaccion(Negocio negocio, GenerarCobroQrRequest request,
                                                OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> buscarCobro(request.id(), negocio)
                .map(anterior -> repetirCobro(anterior, request))
                .orElseGet(() -> guardar(CobroQr.nuevo(request.id(), negocio.getId(),
                        negocio.destinoDeCobro(), request.importe(), ahora))));
    }

    private CobroQr guardarNuevoEnTransaccion(Negocio negocio, GenerarCobroQrRequest request,
                                               OrdenCobroQr orden, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> buscarCobro(request.id(), negocio)
                .map(anterior -> repetirCobro(anterior, request))
                .orElseGet(() -> guardar(CobroQr.nuevoMercadoPago(request.id(), negocio.getId(),
                        request.importe(), orden, ahora))));
    }

    private OrdenCobroQr crearOrden(GenerarCobroQrRequest request) {
        return pasarelaCobrosQrPort.crear(request.id(), request.importe());
    }

    private CobroQr repetirCobro(CobroQr anterior, GenerarCobroQrRequest request) {
        if (anterior.importe().compareTo(request.importe()) != 0) {
            throw new DuplicateResourceException("Este cobro con QR ya se generó por otro importe");
        }
        return anterior;
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
